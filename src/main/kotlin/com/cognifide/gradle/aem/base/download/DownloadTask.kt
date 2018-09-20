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
        clean()

        val shell = prepareShellPackage()
        val sync = InstanceSync(project, instance).apply {
            connectionTimeout = config.downloadConnectionTimeout
        }

        val packagePath = sync.uploadPackage(shell).path

        try {
            sync.buildPackage(packagePath)

            val packageFile = File(taskDir, FilenameUtils.getName(packagePath))
            sync.downloadPackage(packagePath, packageFile)

            if (config.downloadExtract) {
                val jcrRoot = prepareJcrRoot()
                extractContents(packageFile, jcrRoot)
            }

            AemNotifier.of(project).default("Package downloaded", packageFile.name)
        } finally {
            sync.deletePackage(packagePath)
        }
    }

    private fun clean() {
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

    private fun extractContents(downloadedPackage: File, jcrRoot: File) {
        logger.info("Extracting contents of $downloadedPackage into $jcrRoot")
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
            logger.info("Deleting contents of $jcrRoot")
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()
        return jcrRoot
    }
}