package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.utils.filterNotNull
import com.cognifide.gradle.common.utils.Patterns
import org.gradle.api.file.DirectoryProperty
import java.io.File
import java.io.FileFilter
import java.util.*

class MvnBuild(val aem: AemExtension, val rootDir: DirectoryProperty) {

    val project = aem.project

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

    val userDir = project.objects.directoryProperty().dir(System.getProperty("user.home"))

    val repositoryDir = userDir.map { it.dir(".m2/repository") }

    val modules = aem.obj.list<MvnModule> { convention(listOf()) }

    fun module(dir: File, options: MvnModule.() -> Unit) {
        val name = dir.name // TODO support nested modules
        val projectPath = if (project.rootProject == project) ":$name" else "${project.path}:$name"
        val dirProp = project.objects.directoryProperty().apply { set(dir) }

        project.project(projectPath) { subproject ->
            subproject.plugins.apply(CommonPlugin::class.java)
            MvnModule(this, name, dirProp, subproject).apply(options).also { modules.add(it) }
        }
    }

    fun module(name: String) = modules.map {
        it.firstOrNull { m -> m .name == name } ?: throw MvnException("Maven module named '$name' is not defined!")
    }

    val zipModuleNames = aem.obj.strings {
        convention(listOf("ui.apps", "ui.content", "ui.content.*", "dispatcher", "dispatcher.*"))
    }

    val jarModuleNames = aem.obj.strings {
        convention(listOf("core", "bundle.*"))
    }

    val frontendModuleNames = aem.obj.strings {
        convention(listOf("ui.frontend", "ui.frontend.*"))
    }

    // TODO maybe discover using *.dot file and by transforming it
    fun discover() {
        rootDir.get().asFile.listFiles(FileFilter { it.isDirectory })?.forEach { dir ->
            when {
                Patterns.wildcard(dir.name, zipModuleNames.get()) -> module(dir) { buildZip() }
                Patterns.wildcard(dir.name, jarModuleNames.get()) -> module(dir) { buildJar() }
                Patterns.wildcard(dir.name, frontendModuleNames.get()) -> module(dir) { buildFrontend() }
            }
        }

        // TODO calculate dependencies between tasks
        // mvn com.github.ferstl:depgraph-maven-plugin:aggregate -Dincludes=com.mysite
        // mvn com.github.ferstl:depgraph-maven-plugin:aggregate -Dincludes=com.mysite -DgraphFormat=json
    }
}