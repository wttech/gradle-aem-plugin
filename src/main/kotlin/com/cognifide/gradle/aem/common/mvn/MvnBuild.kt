package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.utils.filterNotNull
import com.cognifide.gradle.common.common
import org.gradle.api.Task
import org.gradle.api.UnknownProjectException
import java.util.*

class MvnBuild(val aem: AemExtension) {

    val project = aem.project

    val tasks get() = project.common.tasks

    val projectPathPrefix get() = if (project.rootProject == project) ":" else "${project.path}:"

    val rootDir = aem.obj.dir {
        set(aem.project.projectDir)
    }

    val rootModule get() = module(MvnModule.NAME_ROOT)

    val version get() = rootModule.get().gav.get().version

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

    val depGraph by lazy { MvnDepGraph(this) }

    val userDir = project.objects.directoryProperty().dir(System.getProperty("user.home"))

    val repositoryDir = userDir.map { it.dir(".m2/repository") }

    val modules = aem.obj.list<MvnModule> { convention(listOf()) }.apply {
        // TODO module(MvnModule.NAME_ROOT) { dir.set(rootDir) }
    }

    fun module(name: String, options: MvnModule.() -> Unit) {
        val dirSubPath = name.replace(":", "/")
        val projectSubPath = dirSubPath.replace("/", ":").replace("\\", ":")
        val projectPath = "$projectPathPrefix${projectSubPath}"

        project.project(projectPath) { subproject ->
            subproject.plugins.apply(CommonPlugin::class.java)
            MvnModule(this, name, subproject).apply {
                dir.set(if (name == MvnModule.NAME_ROOT) rootDir else rootDir.dir(dirSubPath))
                options()
            }.also { modules.add(it) }
        }
    }

    fun module(name: String) = modules.map {
        it.firstOrNull { m -> m.name == name } ?: throw MvnException("Maven module named '$name' is not defined!")
    }

    fun discover() {
        try {
            depGraph.moduleArtifacts.get().forEach { (name, extensions) ->
                module(name) {
                    extensions.forEach { extension ->
                        when {
                            extension == MvnModule.ARTIFACT_POM -> buildPom()
                            extension == MvnModule.ARTIFACT_ZIP && frontendIndicator.get() -> buildFrontend(extension)
                            else -> buildArtifact(extension)
                        }
                    }
                }
            }
        } catch (e: UnknownProjectException) {
            val settingsFile = project.rootProject.file("settings.gradle.kts")
            val settingsLines = depGraph.projectPaths.get().joinToString("\n") { """include("$it")""" }
            throw MvnException(
                listOf(
                    "Maven build powered by Gradle AEM Plugin needs to have defined subprojects in Gradle settings file as prerequisite.",
                    "Ensure having following lines in file: $settingsFile",
                    "",
                    settingsLines,
                    ""
                ).joinToString("\n")
            )
        }

        project.gradle.projectsEvaluated {
            depGraph.artifactDependencies.get().forEach { (dep1, dep2) ->
                val task1 = tasks.pathed<Task>("$projectPathPrefix${dep1}")
                val task2 = tasks.pathed<Task>("$projectPathPrefix${dep2}")
                task1.configure { it.dependsOn(task2) }
            }
        }
    }
}
