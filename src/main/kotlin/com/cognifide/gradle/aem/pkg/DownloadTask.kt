package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import org.apache.commons.io.FilenameUtils
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import java.io.File

open class DownloadTask : Zip(), AemTask {

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DOWNLOAD = "download"
        const val CLASSIFIER_SHELL = "downloadShell"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    private val prepareDir = AemTask.taskDir(project, PrepareTask.NAME)

    @Internal
    val props = PropertyParser(project)

    private val checkoutFilter by lazy { VltFilter.of(project) }

    private val instance: Instance by lazy { Instance.single(project) }

    init {
        description = "Builds and downloads CRX package from remote instance"
        group = AemTask.GROUP

        baseName = config.packageName
        isZip64 = true

        // Empty package uploaded to aem will have 'shell' suffix
        classifier = CLASSIFIER_SHELL

        // Take only first filter.xml file from definition
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        //Load zip configuration only when the task is in the graph
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(this)) {
                this.specShellPackage()
            }
        }

        // Task is always executed when in the graph
        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun copy() {
        // Prepare shell package
        super.copy()

        val sync = InstanceSync(project, instance)

        logger.lifecycle("Uploading package $archivePath")
        val packagePath = sync.uploadPackage(archivePath).path

        logger.lifecycle("Building remote package $packagePath")
        sync.buildPackage(packagePath)

        val packageFile = File(destinationDir, FilenameUtils.getName(packagePath))
        logger.lifecycle("Downloading remote package $packagePath to $packageFile")
        sync.downloadPackage(packagePath, packageFile)

        if (config.downloadExtract) {
            val jcrRoot = prepareJcrRoot()
            extractContents(packageFile, jcrRoot)
        }
    }

    private fun specShellPackage() {
        into(PackagePlugin.VLT_PATH) { spec ->
            spec.from(checkoutFilter.file)
            rename(checkoutFilter.file.name, "filter.xml")
        }
        into("") { spec ->
            spec.from(prepareDir)
            spec.eachFile {
                FileContentReader.filter(it) {
                    // Updating version string for package to contain '-download' suffix
                    props.expandPackage(it, mapOf("project.version" to "${project.version}-$CLASSIFIER_DOWNLOAD"), path)
                }
            }
        }
    }

    private fun extractContents(downloadedPackage: File, jcrRoot: File) {
        logger.lifecycle("Extracting contents of $downloadedPackage into $jcrRoot")
        project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(project.zipTree(downloadedPackage.path))
                    .include("${PackagePlugin.JCR_ROOT}/**")
        }
    }

    private fun prepareJcrRoot(): File {
        val content = File(config.contentPath)
        val jcrRoot = File(content, PackagePlugin.JCR_ROOT)

        if (jcrRoot.exists() && props.isForce()) {
            logger.lifecycle("Deleting contents of $jcrRoot")
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()
        return jcrRoot
    }
}