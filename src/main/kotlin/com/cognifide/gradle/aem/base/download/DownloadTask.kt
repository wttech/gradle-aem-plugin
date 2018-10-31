package com.cognifide.gradle.aem.base.download

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemRetry
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

// TODO combine it with checkout task
open class DownloadTask : AemDefaultTask() {

    private val taskDir = AemTask.taskDir(project, name)

    private val shellDir = AemTask.temporaryDir(project, name, CLASSIFIER_SHELL)

    private val checkoutFilter by lazy { VltFilter.of(project) }

    private val instance: Instance by lazy { Instance.single(project) }

    /**
     * Repeat download when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var downloadRetry = aem.retry { afterSquaredSecond(aem.props.long("aem.download.retry", 3)) }

    /**
     * Extract the contents of package downloaded using aemDownload task to current project jcr_root directory
     * This operation can be modified using -Paem.force command line to replace the contents of jcr_root directory with
     * package content
     */
    @Input
    var downloadExtract = aem.props.boolean("aem.download.extract", true)

    /**
     * In case of downloading big CRX packages, AEM could respond much slower so that special
     * timeout is covering such edge case.
     */
    @Input
    var downloadConnectionTimeout = aem.props.int("aem.download.connectionTimeout", 60000)

    init {
        description = "Builds and downloads CRX package from remote instance"
        group = AemTask.GROUP

        // Task is always executed when in the graph
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun download() {
        cleanPackages()

        val shellPackageFile = prepareShellPackage()
        val sync = InstanceSync(project, instance).apply {
            connectionTimeout = downloadConnectionTimeout
        }

        logger.lifecycle("Uploading then building package to be downloaded: $shellPackageFile")

        val packagePath = sync.uploadPackage(shellPackageFile, true, AemRetry()).path
        val packageFile = File(taskDir, FilenameUtils.getName(packagePath))

        try {
            sync.buildPackage(packagePath)
            sync.downloadPackage(packagePath, packageFile, downloadRetry)

            if (downloadExtract) {
                val jcrRoot = File("${aem.compose.contentPath}/${PackagePlugin.JCR_ROOT}")
                logger.lifecycle("Extracting package $packageFile to $jcrRoot")

                extractDownloadedPackage(packageFile, jcrRoot)
            }

            aem.notifier.notify("Package downloaded", packageFile.name)
        } finally {
            logger.lifecycle("Cleaning downloaded package: $packageFile")

            sync.deletePackage(packagePath)
        }
    }

    private fun cleanPackages() {
        GFileUtils.cleanDirectory(taskDir)
    }

    private fun prepareShellPackage(): File {
        val zipResult = File(taskDir, "$CLASSIFIER_SHELL.zip")
        val vltDir = File(shellDir, PackagePlugin.VLT_PATH)
        val jcrRoot = File(shellDir, PackagePlugin.JCR_ROOT)
        vltDir.mkdirs()
        jcrRoot.mkdirs()

        checkoutFilter.file.copyTo(File(vltDir, "filter.xml"))
        FileOperations.copyResources(PackagePlugin.VLT_PATH, vltDir, true)

        FileOperations.amendFiles(vltDir, listOf("**/*.xml")) { file, content ->
            aem.props.expandPackage(content, mapOf("project.version" to "${project.version}-$CLASSIFIER_DOWNLOAD"), file.absolutePath)
        }

        ZipUtil.pack(shellDir, zipResult)
        return zipResult
    }

    private fun extractDownloadedPackage(downloadedPackage: File, jcrRoot: File) {
        if (jcrRoot.exists() && aem.props.isForce()) {
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()

        project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(project.zipTree(downloadedPackage.path))
                    .include("${PackagePlugin.JCR_ROOT}/**")
        }
    }

    companion object {

        const val NAME = "aemDownload"

        const val CLASSIFIER_DOWNLOAD = "download"

        const val CLASSIFIER_SHELL = "downloadShell"

    }

}