package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.Formats
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

// TODO extract download related logic to nested object
open class CheckoutTask : VltTask() {

    @Input
    var instance = aem.instanceAny()

    @Input
    var filter = aem.filter()

    /**
     * Determines a method of getting JCR content from remote invgj 8
     */
    @Input
    var type = Type.PACKAGE_DOWNLOAD

    /**
     * Repeat download when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var downloadRetry = aem.retry { afterSquaredSecond(aem.props.long("aem.checkout.download.retry", 3)) }

    /**
     * Extract the contents of package downloaded using aemDownload task to current project jcr_root directory
     * This operation can be modified using -Paem.force command line to replace the contents of jcr_root directory with
     * package content
     */
    @Input
    var downloadExtract = aem.props.boolean("aem.checkout.download.extract", true)

    /**
     * In case of downloading big CRX packages, AEM could respond much slower so that special
     * timeout is covering such edge case.
     */
    @Input
    var downloadConnectionTimeout = aem.props.int("aem.checkout.download.connectionTimeout", 60000)

    private val downloadRoot = AemTask.taskDir(project, name)

    private val downloadShellDir = AemTask.temporaryDir(project, name, DOWNLOAD_SHELL_CLASSIFIER)

    init {
        description = "Check out JCR content from running AEM instance."
    }

    fun useVltCheckout() {
        type = Type.VLT_CHECKOUT
    }

    fun usePackageDownload() {
        type = Type.PACKAGE_DOWNLOAD
    }

    @TaskAction
    override fun perform() {
        when (type) {
            Type.VLT_CHECKOUT -> performVltCheckout()
            Type.PACKAGE_DOWNLOAD -> performPackageDownload()
        }
    }

    private fun performVltCheckout() {
        vlt.apply {
            command = "--credentials ${instance.credentials} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
        aem.notifier.notify("Checked out JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(vlt.contentPath, project)}")
    }

    private fun performPackageDownload() {
        cleanDownload()

        val shellPackageFile = prepareDownloadShellPackage()
        val sync = InstanceSync(project, instance).apply {
            connectionTimeout = downloadConnectionTimeout
        }

        logger.lifecycle("Uploading then building package to be downloaded: $shellPackageFile")

        val packagePath = sync.uploadPackage(shellPackageFile).path
        val packageFile = File(downloadRoot, FilenameUtils.getName(packagePath))

        try {
            sync.buildPackage(packagePath)
            sync.downloadPackage(packagePath, packageFile, downloadRetry)

            if (downloadExtract) {
                val jcrRoot = File(aem.config.packageJcrRoot)
                logger.lifecycle("Extracting package $packageFile to $jcrRoot")

                extractDownloadedPackage(packageFile, jcrRoot)
            }

            aem.notifier.notify("Package downloaded", packageFile.name)
        } finally {
            logger.lifecycle("Cleaning downloaded package: $packageFile")

            sync.deletePackage(packagePath)
        }
    }

    private fun cleanDownload() {
        GFileUtils.cleanDirectory(downloadRoot)
    }

    private fun prepareDownloadShellPackage(): File {
        val zipResult = File(downloadRoot, "$DOWNLOAD_SHELL_CLASSIFIER.zip")
        val vltDir = File(downloadShellDir, PackagePlugin.VLT_PATH)
        val jcrRoot = File(downloadShellDir, PackagePlugin.JCR_ROOT)
        vltDir.mkdirs()
        jcrRoot.mkdirs()

        filter.file.copyTo(File(vltDir, "filter.xml"))
        FileOperations.copyResources(PackagePlugin.VLT_PATH, vltDir, true)

        FileOperations.amendFiles(vltDir, listOf("**/*.xml")) { file, content ->
            aem.props.expandPackage(content, mapOf("project.version" to "${project.version}-$DOWNLOAD_CLASSIFIER"), file.absolutePath)
        }

        ZipUtil.pack(downloadShellDir, zipResult)
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

    enum class Type {
        VLT_CHECKOUT,
        PACKAGE_DOWNLOAD
    }

    companion object {
        const val NAME = "aemCheckout"

        const val DOWNLOAD_CLASSIFIER = "download"

        const val DOWNLOAD_SHELL_CLASSIFIER = "downloadShell"

    }

}