package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.tasks.InstanceFileSync
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.common.common
import org.gradle.api.Project
import org.gradle.api.Task
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

    var inputFilter: PatternFilterable.() -> Unit = { exclude(inputPatterns.get())  }

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

            // temporary files
            "**/node_modules/**",
            "**/node_modules",
            "**/node/**",
            "**/node",
            "**/*.log",
            "**/*.tmp",

            // generated files
            "**/package-lock.json"
        ))
    }

    val outputFiles get() = project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(dir.dir("target"))
    }

    fun targetFile(extension: String) = targetDir.map { it.file("${gav.get().artifactId}-${gav.get().version ?: build.version}.$extension") }

    fun buildPom() = exec(ARTIFACT_POM) {
        description = "Installs POM to local repository"
        moreArgs(listOf("-N"))
        inputs.file(pom)
        outputs.dir(repositoryDir)
    }

    fun buildFrontend(extension: String = ARTIFACT_ZIP, options: Task.() -> Unit = {}): TaskProvider<Exec> = exec(extension) {
        description = "Builds frontend"
        moreArgs(build.frontendProfiles.get().map { "-P$it" })
        inputs.property("profiles", build.frontendProfiles.get())
        inputs.files(inputFiles)
        outputs.file(targetFile(extension))
        options()
    }

    fun buildJar(options: Task.() -> Unit = {}) = buildArtifact(ARTIFACT_JAR, options)

    fun buildZip(options: Task.() -> Unit = {}) = buildArtifact(ARTIFACT_ZIP, options)

    fun buildArtifact(extension: String, options: Task.() -> Unit = {}): TaskProvider<Exec> = exec(extension) {
        description = "Builds artifact '$extension'"
        val outputFile = targetFile(extension)
        inputs.files(inputFiles)
        outputs.file(outputFile)
        doLast { aem.common.checksumFile(outputFile.get().asFile) }
        options()
    }

    fun exec(name: String, options: Exec.() -> Unit) = tasks.register<Exec>(name) {
        commonOptions()
        executable("mvn")
        moreArgs(listOf())
        workingDir(dir)
        options()
    }.also { task ->
        tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    fun deployPackage(artifactTask: TaskProvider<Exec>, options: InstanceFileSync.() -> Unit = {}) = tasks.register<InstanceFileSync>("deploy") {
        commonOptions()
        description = "Deploys AEM package to instance"
        sync.deployPackage(artifactTask)
        dependsOn(artifactTask)
        apply(build.packageDeployOptions)
        options()
    }

    fun syncPackage(options: PackageSync.() -> Unit = {}) = tasks.register<PackageSync>("sync") {
        commonOptions()
        contentDir(dir.dir(build.packageContentPath.get()).get())
        downloader {
            definition {
                destinationDirectory.set(build.rootDir.dir("build/sync/$name"))
            }
        }
        options()
    }

    val commonArgs = aem.obj.strings {
        convention(listOf("-B", "-T", "2C"))
        aem.prop.string("mvn.commonArgs")?.let { set(it.split(" ")) }
    }

    fun Task.commonOptions() {
        group = AemTask.GROUP
    }

    fun Exec.moreArgs(args: Iterable<String>) {
        args(commonArgs.get() + listOf("clean", "install") + args)
    }

    companion object {
        const val NAME_ROOT = "root"

        const val ARTIFACT_POM = "pom"

        const val ARTIFACT_ROOT_POM = "$NAME_ROOT:$ARTIFACT_POM"

        const val ARTIFACT_ZIP = "zip"

        const val ARTIFACT_JAR = "jar"
    }
}
