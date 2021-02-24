package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.common
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MvnModule(val build: MvnBuild, val name: String, val dir: DirectoryProperty, val project: Project) {

    val aem = build.aem

    fun other(name: String) = build.module(name)

    val root = other("root")

    val tasks get() = project.common.tasks

    val pom = aem.obj.file { set(dir.file("pom.xml")) }

    val gav = aem.obj.typed<MvnGav> {
        set(pom.map { MvnGav.readFile(it.asFile) })
    }

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${gav.get().groupId}/${gav.get().artifactId}") })
    }

    val inputFiles get() = project.fileTree(dir).matching { pf ->
        pf.excludeTypicalOutputs()
        pf.exclude("**/package-lock.json")
    }

    val outputFiles get() =  project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(dir.dir("target"))
    }

    fun targetFile(extension: String) = targetDir.map { it.file("${gav.get().artifactId}-${gav.get().version ?: root.get().gav.get().version!!}.$extension") }

    fun installPom() = exec("pom") {
        moreArgs(listOf("-N"))
        inputs.file(pom)
        outputs.dir(repositoryDir)
    }

    val clientlibDir = aem.obj.dir {
        set(dir.dir("src/main/content/jcr_root/apps/${build.appId.get()}/clientlibs/generated"))
    }

    val frontendProfiles = aem.obj.strings {
        set(project.provider {
            mutableListOf<String>().apply {
                if (aem.prop.boolean("mvn.frontend.dev") == true) {
                    add("fedDev")
                }
            }
        })
    }

    fun buildFrontend(options: Task.() -> Unit = {}) = exec("frontend") {
        val clientlibModule = "ui.apps" // TODO discover that

        moreArgs(frontendProfiles.get().map { "-P${it}" })
        inputs.property("profiles", frontendProfiles.get())
        inputs.files(inputFiles)
        outputs.dir(outputFiles)
        outputs.dir(other(clientlibModule).map { it.clientlibDir })
        options()
    }

    fun buildJar(options: Task.() -> Unit = {}) = buildArtifact("jar", options)

    fun buildZip(options: Task.() -> Unit = {}) = buildArtifact("zip", options)

    fun buildArtifact(extension: String, options: Task.() -> Unit = {}) = exec(extension) {
        inputs.files(inputFiles)
        outputs.dir(targetFile(extension))
        options()
    }

    fun exec(name: String, options: Exec.() -> Unit) = tasks.register<Exec>(name) {
        executable("mvn")
        moreArgs(listOf())
        workingDir(dir)
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    val commonArgs = aem.obj.strings {
        convention(listOf())
        aem.prop.string("mvn.commonArgs")?.let { set(it.split(" ")) }
    }

    fun Exec.moreArgs(args: Iterable<String>) {
        args(commonArgs.get() + listOf("clean", "install") + args)
    }
}