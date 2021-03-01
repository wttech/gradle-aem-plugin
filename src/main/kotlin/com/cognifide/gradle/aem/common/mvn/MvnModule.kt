package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.tasks.InstanceFileSync
import com.cognifide.gradle.aem.pkg.tasks.PackageConfig
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.common.common
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MvnModule(val build: MvnBuild, val name: String, val project: Project) {

    val aem = build.aem

    val tasks get() = project.common.tasks

    val dir = aem.obj.dir()

    val pom = aem.obj.file { set(dir.file("pom.xml")) }

    val gav = aem.obj.typed<MvnGav> {
        set(pom.map { MvnGav.readFile(it.asFile) })
    }

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${gav.get().groupId}/${gav.get().artifactId}") })
    }

    val inputFiles get() = project.fileTree(dir).matching(inputFilter)

    var inputFilter: PatternFilterable.() -> Unit = { exclude(inputPatterns.get()) }

    val inputPatterns = aem.obj.strings {
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

    val outputFiles get() = project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(dir.dir("target"))
    }

    var targetFileLocator: MvnModule.(String) -> Provider<RegularFile> = { extension ->
        targetDir.map { it.file("${gav.get().artifactId}-${gav.get().version ?: build.version}.$extension") }
    }

    fun targetFile(extension: String) = targetFileLocator(extension)

    fun buildPom() = exec(ARTIFACT_POM) {
        description = "Installs POM to local repository"
        inputs.file(pom)
        outputs.dir(repositoryDir)
    }

    fun buildJar(options: Exec.() -> Unit = {}) = buildArtifact(ARTIFACT_JAR) {
        description = "Builds JAR file"
        options()
    }

    fun buildZip(options: Exec.() -> Unit = {}) = buildArtifact(ARTIFACT_ZIP) {
        description = "Builds ZIP archive"
        options()
    }

    fun buildFrontend(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(ARTIFACT_ZIP) {
        description = "Builds AEM frontend"
        moreArgs(build.frontendProfiles.get().map { "-P$it" })
        inputs.property("profiles", build.frontendProfiles)
        inputs.files(inputFiles)
        outputs.file(targetFile(ARTIFACT_ZIP))
        options()
    }

    fun configurePackage() {
        val buildPackage = buildPackage()
        deployPackage(targetFile(ARTIFACT_ZIP)) {
            dependsOn(buildPackage)
            apply(build.packageDeployOptions)
        }
        syncPackage()
        syncConfig()
    }

    fun buildPackage(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(ARTIFACT_ZIP) {
        description = "Builds AEM package"
        val outputFile = targetFile(ARTIFACT_ZIP)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        doLast { aem.common.checksumFile(outputFile.get().asFile, true) }
        options()
    }

    fun deployPackage(zip: Any, options: InstanceFileSync.() -> Unit = {}) = tasks.register<InstanceFileSync>("deploy") {
        commonOptions()
        description = "Deploys AEM package to instance"
        sync.deployPackage(zip)
        apply(build.packageDeployOptions)
        options()
    }

    fun syncPackage(options: PackageSync.() -> Unit = {}) = tasks.register<PackageSync>("sync") {
        commonOptions()
        contentDir(dir.dir(build.packageContentPath.get()).get())
        options()
    }

    fun syncConfig(options: PackageConfig.() -> Unit = {}) = tasks.register<PackageConfig>("config") {
        commonOptions()
        saveDir.convention(dir.map { it.dir("${build.packageContentPath.get()}/jcr_root/apps/${build.appId.get()}/osgiconfig/config") })
        pid.convention(build.groupId.map { "$it.*" })
        apply(build.packageConfigOptions)
        options()
    }

    fun buildArtifact(extension: String, options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(extension) {
        description = "Builds artifact '$extension'"
        val outputFile = targetFile(extension)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        options()
    }

    fun buildModule(options: Exec.() -> Unit = {}) = exec("module") {
        description = "Builds module"
        inputs.files(inputFiles)
        outputs.files(outputFiles)
        options()
    }

    fun exec(name: String, options: Exec.() -> Unit) = tasks.register<Exec>(name) {
        commonOptions()
        executable("mvn")
        moreArgs(listOf())
        workingDir(dir)
        apply(build.execOptions)
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    fun Exec.moreArgs(args: Iterable<String>) {
        args(build.execArgs.get() + listOf("clean", "install", "-N") + args)
    }

    fun Task.commonOptions() {
        group = AemTask.GROUP
    }

    override fun toString() = "MvnModule(name='$name', dir=${dir.get().asFile}"

    companion object {
        const val NAME_ROOT = "root"

        const val ARTIFACT_POM = "pom"

        const val ARTIFACT_ROOT_POM = "$NAME_ROOT:$ARTIFACT_POM"

        const val ARTIFACT_ZIP = "zip"

        const val ARTIFACT_JAR = "jar"
    }
}
