package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.utils.filterNotNull
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.utils.using
import org.gradle.api.Task
import org.gradle.api.UnknownProjectException
import org.gradle.api.tasks.TaskProvider
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
            val rff = rf.asFile
            if (!rff.exists()) {
                throw MvnException(listOf(
                    "Maven archetype properties file does not exist '$rff'!",
                    "Consider creating it or specify 'appId' and 'groupId' in build script."
                ).joinToString(" "))
            }

            Properties().apply { rff.bufferedReader().use { load(it) } }
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

    val version get() = MvnGav.readFile(rootDir.get().asFile.resolve("pom.xml")).version
        ?: throw MvnException("Cannot determine Maven build version at path '${rootDir.get().asFile}'!")

    val outputPatterns = aem.obj.strings {
        set(listOf(
            "**/.idea/**",
            "**/.idea",
            "**/.gradle/**",
            "**/.gradle",
            "**/gradle.user.properties",
            "**/gradle/user/**",

            // outputs
            "**/target/**",
            "**/target",
            "**/build/**",
            "**/build",
            "**/dist/**",
            "**/dist",
            "**/generated",
            "**/generated/**",

            "**/package-lock.json",

            // temporary files
            "**/node_modules/**",
            "**/node_modules",
            "**/node/**",
            "**/node",
            "**/*.log",
            "**/*.tmp",
        ))
    }

    val depGraph by lazy { DependencyGraph(this) }

    fun depGraph(options: DependencyGraph.() -> Unit) = depGraph.using(options)

    val moduleResolver = ModuleResolver(this)

    fun moduleResolver(options: ModuleResolver.() -> Unit) = moduleResolver.using(options)

    val userDir = aem.obj.dir { set(project.file(System.getProperty("user.home"))) }

    val repositoryDir = userDir.map { it.dir(".m2/repository") }

    val modules = aem.obj.list<MvnModule> { convention(listOf()) }

    private var moduleOptions: MvnModule.() -> Unit = {}

    fun module(options: MvnModule.() -> Unit) {
        this.moduleOptions = options
    }

    fun module(artifactId: String, options: MvnModule.() -> Unit) {
        val descriptor = moduleResolver.all.get().firstOrNull { it.artifactId == artifactId }
            ?: throw MvnException("Maven module with artifactId '$artifactId' not found!")

        module(descriptor, options)
    }

    private fun module(descriptor: ModuleDescriptor, options: MvnModule.() -> Unit) {
        project.project(descriptor.projectPath) { subproject ->
            subproject.plugins.apply(CommonPlugin::class.java)
            subproject.aem.common {
                tmpDir.set(project.layout.buildDirectory.dir("mvnBuild/${descriptor.artifactId}"))
            }
            MvnModule(this, descriptor, subproject).apply {
                moduleOptions()
                options()
            }.also { modules.add(it) }
        }
    }

    fun module(artifactId: String) = modules.map {
        it.firstOrNull { m -> m.descriptor.artifactId == artifactId }
            ?: throw MvnException("Maven module with artifactId '$artifactId' is not defined!")
    }

    fun rootModule(options: MvnModule.() -> Unit) = module(moduleResolver.root.get().artifactId, options)

    val rootModule = modules.map { m -> m.firstOrNull { it.descriptor.root }
        ?: throw MvnException("Maven root module cannot be found!")
    }

    fun discover() {
        try {
            defineModulesResolved()
        } catch (e: UnknownProjectException) {
            val settingsFile = project.rootProject.file("settings.gradle.kts")
            val settingsLines = moduleResolver.projectPaths.get().joinToString("\n") { """include("$it")""" }
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
            defineModuleTaskDependenciesFromGraph()
        }
    }

    private fun defineModulesResolved() = moduleResolver.all.get().forEach { descriptor ->
        module(descriptor) {
            when (descriptor.type) {
                ModuleType.POM -> buildPom()
                ModuleType.JAR -> buildJar()
                ModuleType.PACKAGE -> configurePackage()
                ModuleType.FRONTEND -> buildFrontend()
                ModuleType.DISPATCHER -> buildZip()
                ModuleType.OTHER -> buildModule()
            }
        }
    }

    private fun defineModuleTaskDependenciesFromGraph() = depGraph.all.get().forEach { (a1, a2) ->
        val tp1 = findModuleTask(a1)
        val tp2 = findModuleTask(a2)

        tp1.configure { t1 ->
            t1.dependsOn(tp2)
            t1.inputs.files(tp2.map { it.outputs.files })
        }
    }

    private fun findModuleTask(artifact: Artifact): TaskProvider<Task> {
        val descriptor = moduleResolver.all.get().firstOrNull { it.artifact == artifact }
            ?: throw MvnException("Cannot find task for artifact '${artifact.notation}'!")
        return tasks.pathed(descriptor.taskPath)
    }

    override fun toString() = "MvnBuild(rootDir=${rootDir.get().asFile}, appId=${appId.orNull}, groupId=${groupId.orNull})"
}
