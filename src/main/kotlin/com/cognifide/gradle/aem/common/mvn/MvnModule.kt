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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MvnModule(val build: MvnBuild, val descriptor: ModuleDescriptor, val project: Project) {

    val aem = build.aem

    val appId = aem.obj.string {
        set(build.appId)
    }

    val tasks get() = project.common.tasks

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${descriptor.groupId.replace(".", "/")}/${descriptor.artifactId}/${descriptor.version}") })
    }

    val repositoryPom = aem.obj.file {
        set(repositoryDir.map { it.file("${descriptor.artifactId}-${descriptor.version}.pom") })
    }

    val inputFiles get() = project.fileTree(descriptor.dir).matching(inputFilter)

    var inputFilter: PatternFilterable.() -> Unit = { exclude(outputExclusions.get()) }

    val outputExclusions = aem.obj.strings { set(build.outputExclusions) }

    val outputFiles get() = project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(descriptor.dir.resolve("target"))
    }

    fun targetFileLocator(locator: MvnModule.(extension: String) -> Provider<RegularFile>) {
        this.targetFileLocator = locator
    }

    private var targetFileLocator: MvnModule.(String) -> Provider<RegularFile> = { extension ->
        targetDir.map { it.file("${descriptor.artifactId}-${descriptor.version}.$extension") }
    }

    fun targetFile(extension: String) = targetFileLocator(extension)

    val profiles = aem.obj.strings {
        convention(listOf())
        aem.prop.list("mvn.profiles")?.let { set(it) }
    }

    val profilesAvailable = profiles.map { ps ->
        ps.filter { descriptor.hasProfile(it) }.map { "-P$it" }
    }

    fun buildPom() = exec(Artifact.POM) {
        description = "Installs POM to local repository"
        args("clean", "install")
        inputs.file(descriptor.pom)
        outputs.dir(repositoryDir)
    }

    fun buildJar(options: MvnExec.() -> Unit = {}) = buildArtifact(Artifact.JAR) {
        description = "Builds JAR file"
        options()
    }

    fun buildZip(options: MvnExec.() -> Unit = {}) = buildArtifact(Artifact.ZIP) {
        description = "Builds ZIP archive"
        options()
    }

    fun buildArtifact(extension: String, options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(extension) {
        description = "Builds artifact '$extension'"
        args("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(extension))
        outputs.dir(repositoryDir)
        options()
    }

    fun buildFrontend(options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(Artifact.ZIP) {
        description = "Builds AEM frontend"
        args("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(Artifact.ZIP))
        outputs.dir(repositoryDir)
        options()
    }

    fun configurePackage() {
        val buildPackage = buildPackage()
        deployPackage(targetFile(Artifact.ZIP)) { dependsOn(buildPackage) }
        syncPackage()
        syncConfig()
    }

    fun buildPackage(options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(Artifact.ZIP) {
        description = "Builds AEM package"
        args("clean", "install")
        val outputFile = targetFile(Artifact.ZIP)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        outputs.dir(repositoryDir)
        doLast { aem.common.checksumFile(outputFile.get().asFile, true) }
        options()
    }

    fun deployPackage(zip: Any, options: InstanceFileSync.() -> Unit = {}) = tasks.register<InstanceFileSync>(TASK_PACKAGE_DEPLOY) {
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

    fun syncPackage(options: PackageSync.() -> Unit = {}) = tasks.register<PackageSync>(TASK_PACKAGE_SYNC) {
        commonOptions()
        contentDir(descriptor.dir.resolve(build.contentPath.get()))
        options()
    }

    fun syncConfig(options: PackageConfig.() -> Unit = {}) = tasks.register<PackageConfig>(TASK_PACKAGE_CONFIG) {
        commonOptions()
        saveDir.fileProvider(appId.map { id -> descriptor.determineOsgiConfigPath(id) })
        pid.convention(build.groupId.map { "$it.*" })
        options()
    }

    fun buildModule(options: MvnExec.() -> Unit = {}) = exec(TASK_MODULE) {
        description = "Builds module"
        args("clean", "install")
        inputs.files(inputFiles)
        outputs.files(outputFiles)
        outputs.dir(repositoryDir)
        options()
    }

    fun exec(name: String, options: MvnExec.() -> Unit) = tasks.register<MvnExec>(name) {
        commonOptions()
        workingDir.set(descriptor.dir)
        inputs.property("profiles", profilesAvailable)
        doFirst { args(profilesAvailable.get()) }
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    fun exec(options: MvnExec.() -> Unit) = tasks.typed(options)

    fun Task.commonOptions() {
        group = AemTask.GROUP
    }

    override fun toString() = "MvnModule(projectPath='${project.path}' descriptor=$descriptor)"

    companion object {
        const val TASK_PACKAGE_DEPLOY = "deploy"

        const val TASK_PACKAGE_SYNC = "sync"

        const val TASK_PACKAGE_CONFIG = "config"

        const val TASK_MODULE = "module"
    }
}
