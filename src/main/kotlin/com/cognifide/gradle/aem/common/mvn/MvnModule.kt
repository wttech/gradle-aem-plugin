package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceFileSync
import com.cognifide.gradle.aem.common.utils.fileNames
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.LocalInstancePlugin
import com.cognifide.gradle.aem.instance.tasks.InstanceProvision
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.tasks.PackageConfig
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.mvn.MvnExec
import com.cognifide.gradle.common.pathPrefix
import com.cognifide.gradle.common.pluginProject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
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

    fun file(path: String) = descriptor.dir.resolve(path)

    val tasks get() = project.common.tasks

    fun task(name: String) = tasks.named<Task>(name)

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${descriptor.groupId.replace(".", "/")}/${descriptor.artifactId}/${descriptor.version}") })
    }

    val repositoryPom = aem.obj.file {
        set(repositoryDir.map { it.file("${descriptor.artifactId}-${descriptor.version}.pom") })
    }

    val inputFiles = aem.obj.files { from(project.fileTree(descriptor.dir).matching { inputFilter(it) }) }

    var inputFilter: PatternFilterable.() -> Unit = { exclude(outputExclusions.get()) }

    val outputExclusions = aem.obj.strings { set(build.outputExclusions) }

    val outputFiles = aem.obj.files { from(targetDir) }

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

    val skipTests = aem.obj.boolean {
        set(build.skipTests)
    }

    val profiles = aem.obj.strings {
        set(build.profiles)
    }

    private val profileArgs = profiles.map { ps ->
        ps.filter { descriptor.hasProfile(it) }.map { "-P$it" }
    }

    fun buildPom() = exec(ArtifactType.POM.task) {
        description = "Installs POM to local repository"
        invoker.args("clean", "install")
        inputs.file(descriptor.pom)
        outputs.file(repositoryPom)
    }

    fun buildJar(options: MvnExec.() -> Unit = {}) = buildArtifact(ArtifactType.JAR) {
        description = "Builds JAR file"
        options()
    }

    fun buildZip(options: MvnExec.() -> Unit = {}) = buildArtifact(ArtifactType.ZIP) {
        description = "Builds ZIP archive"
        options()
    }

    fun buildArtifact(type: ArtifactType, options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(type.task) {
        description = "Builds artifact '${type.extension}'"
        invoker.args("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(type.extension))
        outputs.file(repositoryPom)
        options()
    }

    fun buildFrontend(options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(ArtifactType.ZIP.task) {
        description = "Builds AEM frontend"
        invoker.args("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(ArtifactType.ZIP.extension))
        outputs.file(repositoryPom)
        options()
    }

    fun configurePackage() {
        val buildPackage = buildPackage()
        deployPackage(targetFile(ArtifactType.ZIP.extension)) { dependsOn(buildPackage) }
        syncPackage()
        syncConfig()
    }

    fun buildPackage(options: MvnExec.() -> Unit = {}): TaskProvider<MvnExec> = exec(ArtifactType.ZIP.task) {
        description = "Builds CRX package"
        invoker.args("clean", "install")
        val outputFile = targetFile(ArtifactType.ZIP.extension)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        outputs.file(repositoryPom)

        doLast(object : Action<Task> { // https://docs.gradle.org/7.4.1/userguide/validation_problems.html#implementation_unknown
            override fun execute(task: Task) {
                aem.common.checksumFile(outputFile.get().asFile, true)
            }
        })
        options()
    }

    fun deployPackage(zip: Any, options: InstanceFileSync.() -> Unit = {}) = tasks.register<InstanceFileSync>(TASK_PACKAGE_DEPLOY) {
        commonOptions()
        description = "Deploys CRX package to instance"
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
        doLast(object : Action<Task> { // https://docs.gradle.org/7.4.1/userguide/validation_problems.html#implementation_unknown
            override fun execute(task: Task) {
                common.notifier.notify("Package deployed", "${files.fileNames} on ${instances.names}")
            }
        })
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

    fun runModule(options: MvnExec.() -> Unit = {}) = exec(ArtifactType.RUN.task) {
        description = "Run module"
        invoker.args("clean", "install")
        inputs.files(inputFiles)
        outputs.files(outputFiles)
        outputs.file(repositoryPom)
        options()
    }

    fun exec(name: String, options: MvnExec.() -> Unit) = tasks.register<MvnExec>(name) {
        commonOptions()
        invoker {
            workingDir.set(build.rootDir)
            args.addAll("-N", "-f", descriptor.pom.absolutePath)
            args.addAll(profileArgs)
            if (skipTests.get()) args.add("-DskipTests")
            aem.prop.string("mvnBuild.args")?.let { args.addAll(it.split(" ")) }
        }
        inputs.property("pomPath", descriptor.pom.absolutePath)
        inputs.property("profiles", profileArgs)
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure { it.dependsOn(task) }
    }

    fun exec(options: MvnExec.() -> Unit) = tasks.typed(options)

    fun execGraphReady(options: MvnExec.(TaskExecutionGraph) -> Unit) {
        project.gradle.taskGraph.whenReady { graph ->
            tasks.typed<MvnExec>().configureEach { options(it, graph) }
        }
    }

    fun Task.commonOptions() {
        group = AemTask.GROUP
    }

    override fun toString() = "MvnModule(projectPath='${project.path}' descriptor=$descriptor)"

    companion object {
        const val TASK_PACKAGE_DEPLOY = PackageDeploy.NAME

        const val TASK_PACKAGE_SYNC = PackageSync.NAME

        const val TASK_PACKAGE_CONFIG = PackageConfig.NAME
    }
}
