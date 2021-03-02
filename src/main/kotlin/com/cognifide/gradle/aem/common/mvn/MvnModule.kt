package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.tasks.InstanceFileSync
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.LocalInstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.PackageConfig
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.pathPrefix
import com.cognifide.gradle.common.pluginProject
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MvnModule(val build: MvnBuild, val descriptor: ModuleDescriptor, val project: Project) {

    val aem = build.aem

    val tasks get() = project.common.tasks

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${descriptor.groupId.replace(".", "/")}/${descriptor.artifactId}/${descriptor.version}") })
    }

    val repositoryPom = aem.obj.file {
        set(repositoryDir.map { it.file("${descriptor.artifactId}-${descriptor.version}.pom") })
    }

    val inputFiles get() = project.fileTree(descriptor.dir).matching(inputFilter)

    var inputFilter: PatternFilterable.() -> Unit = { exclude(outputPatterns.get()) }

    val outputPatterns = aem.obj.strings { set(build.outputPatterns) }

    val outputFiles get() = project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(descriptor.dir.resolve("target"))
    }

    fun targetFileLocator(locator: (extension: String) -> Provider<RegularFile>) {
        this.targetFileLocator = locator
    }

    private var targetFileLocator: (String) -> Provider<RegularFile> = { extension ->
        targetDir.map { it.file("${descriptor.artifactId}-${descriptor.version}.$extension") }
    }

    fun targetFile(extension: String) = targetFileLocator(extension)

    fun buildPom() = exec(Artifact.POM) {
        description = "Installs POM to local repository"
        combineArgs("clean", "install")
        inputs.file(descriptor.pom)
        outputs.dir(repositoryDir)
    }

    fun buildJar(options: Exec.() -> Unit = {}) = buildArtifact(Artifact.JAR) {
        description = "Builds JAR file"
        options()
    }

    fun buildZip(options: Exec.() -> Unit = {}) = buildArtifact(Artifact.ZIP) {
        description = "Builds ZIP archive"
        options()
    }

    fun buildArtifact(extension: String, options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(extension) {
        description = "Builds artifact '$extension'"
        combineArgs("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(extension))
        outputs.dir(repositoryDir)
        options()
    }

    fun buildFrontend(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(Artifact.ZIP) {
        description = "Builds AEM frontend"
        combineArgs(listOf("clean", "install") + frontendProfiles.get().map { "-P$it" })
        inputs.property("profiles", frontendProfiles)
        inputs.files(inputFiles)
        outputs.file(targetFile(Artifact.ZIP))
        outputs.dir(repositoryDir)
        options()
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

    fun configurePackage() {
        val buildPackage = buildPackage()
        deployPackage(targetFile(Artifact.ZIP)) { dependsOn(buildPackage) }
        syncPackage()
        syncConfig()
    }

    fun buildPackage(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(Artifact.ZIP) {
        description = "Builds AEM package"
        combineArgs("clean", "install")
        val outputFile = targetFile(Artifact.ZIP)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        outputs.dir(repositoryDir)
        doLast { aem.common.checksumFile(outputFile.get().asFile, true) }
        options()
    }

    fun deployPackage(zip: Any, options: InstanceFileSync.() -> Unit = {}) = tasks.register<InstanceFileSync>("deploy") {
        commonOptions()
        description = "Deploys AEM package to instance"
        sync.deployPackage(zip)

        val localInstanceProject = project.pluginProject(LocalInstancePlugin.ID)
        if (localInstanceProject != null) {
            mustRunAfter(listOf(InstanceUp.NAME, InstanceProvision.NAME).map { "${localInstanceProject.pathPrefix}$it" })
        } else {
            val instanceProject = project.pluginProject(InstancePlugin.ID)
            if (instanceProject != null) {
                mustRunAfter(listOf(InstanceProvision.NAME).map { "${instanceProject.pathPrefix}$it" })
            }
        }

        options()
    }

    fun syncPackage(options: PackageSync.() -> Unit = {}) = tasks.register<PackageSync>("sync") {
        commonOptions()
        contentDir(descriptor.dir.resolve("src/main/content"))
        options()
    }

    fun syncConfig(options: PackageConfig.() -> Unit = {}) = tasks.register<PackageConfig>("config") {
        commonOptions()
        saveDir.set(descriptor.dir.resolve("src/main/content/jcr_root/apps/${build.appId.get()}/osgiconfig/config"))
        pid.convention(build.groupId.map { "$it.*" })
        options()
    }

    fun buildModule(options: Exec.() -> Unit = {}) = exec("module") {
        description = "Builds module"
        combineArgs("clean", "install")
        inputs.files(inputFiles)
        outputs.files(outputFiles)
        outputs.dir(repositoryDir)
        options()
    }

    fun exec(name: String, options: Exec.() -> Unit) = tasks.register<Exec>(name) {
        commonOptions()
        executable("mvn")
        combineArgs()
        workingDir(descriptor.dir)
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    val execArgs = aem.obj.strings {
        convention(listOf("-B"))
        aem.prop.string("mvn.execArgs")?.let { set(it.split(" ")) }
    }

    fun Exec.combineArgs(vararg args: String) = combineArgs(args.asIterable())

    fun Exec.combineArgs(extraArgs: Iterable<String>) {
        setArgs(execArgs.get() + listOf("-N") + extraArgs)
    }

    fun Task.commonOptions() {
        group = AemTask.GROUP
    }

    override fun toString() = "MvnModule(projectPath='${project.path}' descriptor=$descriptor)"
}
