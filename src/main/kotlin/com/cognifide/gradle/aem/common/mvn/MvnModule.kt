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

class MvnModule(val build: MvnBuild, val name: String, val project: Project) {

    val aem = build.aem

    val tasks get() = project.common.tasks

    val dir = aem.obj.dir()

    val pom = aem.obj.file { set(dir.file("pom.xml")) }

    val gav = aem.obj.typed<MvnGav> {
        set(pom.map { MvnGav.readFile(it.asFile) })
    }

    val groupId get() = gav.map { it.groupId ?: build.groupId.get() }.get()

    val artifactId get() = gav.map { it.artifactId }.get()

    val version get() = gav.map { it.version ?: build.version }.get()

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${groupId.replace(".", "/")}/$artifactId/$version") })
    }

    val repositoryPom = aem.obj.file {
        set(repositoryDir.map { it.file("$groupId.$artifactId-$version.xml") })
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

    var targetFileLocator: (String) -> Provider<RegularFile> = { extension ->
        targetDir.map { it.file("$artifactId-$version.$extension") }
    }

    fun targetFile(extension: String) = targetFileLocator(extension)

    fun buildPom() = exec(ARTIFACT_POM) {
        description = "Installs POM to local repository"
        combineArgs("clean", "install")
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

    fun buildArtifact(extension: String, options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(extension) {
        description = "Builds artifact '$extension'"
        combineArgs("clean", "install")
        inputs.files(inputFiles)
        outputs.file(targetFile(extension))
        outputs.dir(repositoryDir)
        options()
    }

    fun buildFrontend(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(ARTIFACT_ZIP) {
        description = "Builds AEM frontend"
        combineArgs(listOf("clean", "install") + frontendProfiles.get().map { "-P$it" })
        inputs.property("profiles", frontendProfiles)
        inputs.files(inputFiles)
        outputs.file(targetFile(ARTIFACT_ZIP))
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
        deployPackage(targetFile(ARTIFACT_ZIP)) { dependsOn(buildPackage) }
        syncPackage()
        syncConfig()
    }

    fun buildPackage(options: Exec.() -> Unit = {}): TaskProvider<Exec> = exec(ARTIFACT_ZIP) {
        description = "Builds AEM package"
        combineArgs("clean", "install")
        val outputFile = targetFile(ARTIFACT_ZIP)
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
        contentDir(dir.dir(build.packageContentPath.get()).get())
        options()
    }

    fun syncConfig(options: PackageConfig.() -> Unit = {}) = tasks.register<PackageConfig>("config") {
        commonOptions()
        saveDir.convention(dir.map { it.dir("${build.packageContentPath.get()}/jcr_root/apps/${build.appId.get()}/osgiconfig/config") })
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
        workingDir(dir)
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

    override fun toString() = "MvnModule(name='$name', dir=${dir.get().asFile}"

    companion object {
        const val NAME_ROOT = "root"

        const val ARTIFACT_POM = "pom"

        const val ARTIFACT_ROOT_POM = "$NAME_ROOT:$ARTIFACT_POM"

        const val ARTIFACT_ZIP = "zip"

        const val ARTIFACT_JAR = "jar"
    }
}
