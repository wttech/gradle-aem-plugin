package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.utils.filterNotNull
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.pathPrefix
import com.cognifide.gradle.common.utils.using
import org.gradle.api.Task
import org.gradle.api.UnknownProjectException
import java.util.*

class MvnBuild(val aem: AemExtension) {

    val project = aem.project

    val tasks get() = project.common.tasks

    val rootDir = aem.obj.dir {
        set(aem.project.projectDir)
    }

    fun rootDir(path: String) {
        rootDir.set(aem.project.file(path))
    }

    val archetypePropertiesFile = aem.obj.file {
        set(rootDir.file("archetype.properties"))
    }

    val archetypeProperties = aem.obj.map<String, String> {
        set(archetypePropertiesFile.map { rf ->
            Properties().apply { rf.asFile.bufferedReader().use { load(it) } }
                .filterNotNull()
                .entries.map { (k, v) -> k.toString() to v.toString() }
                .toMap()
        })
    }

    val appId = aem.obj.string {
        set(archetypeProperties.getting("appId"))
    }

    val groupId = aem.obj.string {
        set(archetypeProperties.getting("groupId"))
    }

    val version get() = rootModule.get().gav.get().version
        ?: throw MvnException("Cannot determine Maven build version at path '${rootDir.get().asFile}'!")


    val depGraph by lazy { DependencyGraph(this) }

    fun depGraph(options: DependencyGraph.() -> Unit) = depGraph.using(options)

    val userDir = aem.obj.dir { set(project.file(System.getProperty("user.home"))) }

    val repositoryDir = userDir.map { it.dir(".m2/repository") }

    val modules = aem.obj.list<MvnModule> { convention(listOf()) }

    private var moduleOptions: MvnModule.() -> Unit = {}

    fun module(options: MvnModule.() -> Unit) {
        this.moduleOptions = options
    }

    fun module(name: String, options: MvnModule.() -> Unit) {
        val dirSubPath = name.replace(":", "/")
        val projectSubPath = dirSubPath.replace("/", ":").replace("\\", ":")
        val projectPath = "${project.pathPrefix}$projectSubPath"

        project.project(projectPath) { subproject ->
            subproject.plugins.apply(CommonPlugin::class.java)
            subproject.aem.common {
                tmpDir.set(project.layout.buildDirectory.dir("mvnBuild/$name"))
            }
            MvnModule(this, name, subproject).apply {
                dir.set(if (name == MvnModule.NAME_ROOT) rootDir else rootDir.dir(dirSubPath))
                moduleOptions()
                options()
            }.also { modules.add(it) }
        }
    }

    fun module(name: String) = modules.map {
        it.firstOrNull { m -> m.name == name } ?: throw MvnException("Maven module named '$name' is not defined!")
    }

    fun rootModule(options: MvnModule.() -> Unit) = module(MvnModule.NAME_ROOT, options)

    val rootModule get() = module(MvnModule.NAME_ROOT)

    var packageIndicator: MvnModule.() -> Boolean = {
        dir.get().dir(packageContentPath.get()).asFile.exists() ||
                pom.get().asFile.readText().contains(packagePluginName.get())
    }

    val packageContentPath = aem.obj.string {
        convention("src/main/content")
    }

    val packagePluginName = aem.obj.string {
        convention("-package-maven-plugin")
    }

    var frontendIndicator: MvnModule.() -> Boolean = {
        dir.get().file("clientlib.config.js").asFile.exists()
    }

    fun discover() {
        try {
            defineGraphModules()
        } catch (e: UnknownProjectException) {
            val settingsFile = project.rootProject.file("settings.gradle.kts")
            val settingsLines = depGraph.projectPaths.get().joinToString("\n") { """include("$it")""" }
            throw MvnException(
                listOf(
                    "Maven build powered by ${AemPlugin.NAME} needs to have defined subprojects in Gradle settings file as prerequisite.",
                    "Ensure having following lines in file: $settingsFile",
                    "",
                    settingsLines,
                    ""
                ).joinToString("\n")
            )
        }
        project.gradle.projectsEvaluated {
            defineGraphDependencies()
        }
    }

    private fun defineGraphModules() = depGraph.moduleArtifacts.get().forEach { (moduleName, artifactExtensions) ->
        module(moduleName) {
            artifactExtensions.forEach { extension ->
                when {
                    extension == MvnModule.ARTIFACT_POM -> buildPom()
                    extension == MvnModule.ARTIFACT_ZIP && frontendIndicator(this) -> buildFrontend()
                    extension == MvnModule.ARTIFACT_ZIP && packageIndicator(this) -> configurePackage()
                    extension == MvnModule.ARTIFACT_JAR -> buildJar()
                    else -> buildModule()
                }
            }
        }
    }

    private fun defineGraphDependencies() = depGraph.all.get().forEach { (a1, a2) ->
        val tp1 = tasks.pathed<Task>("${project.pathPrefix}${a1.notation}")
        val tp2 = tasks.pathed<Task>("${project.pathPrefix}${a2.notation}")

        tp1.configure { t1 ->
            t1.dependsOn(tp2)
            t1.inputs.files(tp2.map { it.outputs.files })
        }
    }

    override fun toString() = "MvnBuild(rootDir=${rootDir.get().asFile}, appId=${appId.orNull}, groupId=${groupId.orNull})"
}
