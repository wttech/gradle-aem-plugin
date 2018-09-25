package com.cognifide.gradle.aem.base.download

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

open class DownloadTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DOWNLOAD = "download"
        const val CLASSIFIER_SHELL = "downloadShell"
    }

    private val taskDir = AemTask.taskDir(project, NAME)

    private val shellDir = AemTask.temporaryDir(project, NAME, CLASSIFIER_SHELL)

    private val checkoutFilter by lazy { VltFilter.of(project) }

    private val instance: Instance by lazy { Instance.single(project) }

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
            connectionTimeout = config.downloadConnectionTimeout
        }

        logger.lifecycle("Uploading then building package to be downloaded: $shellPackageFile")

        val packagePath = sync.uploadPackage(shellPackageFile).path
        val packageFile = File(taskDir, FilenameUtils.getName(packagePath))

        try {
            sync.buildPackage(packagePath)
            sync.downloadPackage(packagePath, packageFile)

            if (config.downloadExtract) {
                val jcrRoot = File("${config.contentPath}/${PackagePlugin.JCR_ROOT}")
                logger.lifecycle("Extracting package $packageFile to $jcrRoot")

                extractDownloadedPackage(packageFile, jcrRoot)
            }

            AemNotifier.of(project).default("Package downloaded", packageFile.name)
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
            props.expandPackage(content, mapOf("project.version" to "${project.version}-$CLASSIFIER_DOWNLOAD"), file.absolutePath)
        }

        ZipUtil.pack(shellDir, zipResult)
        return zipResult
    }

    private fun extractDownloadedPackage(downloadedPackage: File, jcrRoot: File) {
        if (jcrRoot.exists() && props.isForce()) {
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()

        project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(project.zipTree(downloadedPackage.path))
                    .include("${PackagePlugin.JCR_ROOT}/**")
        }
    }

}