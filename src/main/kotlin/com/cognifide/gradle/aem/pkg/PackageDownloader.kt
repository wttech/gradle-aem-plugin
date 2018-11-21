package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.base.BaseExtension
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.internal.http.HttpClient
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class PackageDownloader(
        @Internal
        private val project: Project,
        @Internal
        private val temporaryDir: File
) {

    @Internal
    private val aem = BaseExtension.of(project)

    @Internal
    private val shellDir = File(temporaryDir, PKG_CLASSIFIER_SHELL)

    @Input
    var instance: Instance = aem.instanceAny

    @Input
    var filter: VltFilter = aem.filter

    /**
     * Repeat download when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var retry = aem.retry { afterSquaredSecond(aem.props.long("aem.package.download.retry", 3)) }

    /**
     * Extract the contents of package downloaded using aemDownload task to current project jcr_root directory
     * This operation can be modified using -Paem.force command line to replace the contents of jcr_root directory with
     * package content
     */
    @Input
    var extract = aem.props.boolean("aem.package.download.extract", true)

    /**
     * In case of downloading big CRX packages, AEM could respond much slower so that special
     * timeout is covering such edge case.
     */
    @Input
    var httpOptions: HttpClient.() -> Unit = {
        connectionTimeout = aem.props.int("aem.package.download.httpOptions.connectionTimeout", 60000)
    }

    fun download() {
        clean()

        val shellPackageFile = prepareShellPackage()
        val sync = instance.sync.apply(httpOptions)

        aem.logger.lifecycle("Uploading then building package to be downloaded: $shellPackageFile")

        val packagePath = sync.uploadPackage(shellPackageFile).path
        val packageFile = File(temporaryDir, FilenameUtils.getName(packagePath))

        try {
            sync.buildPackage(packagePath)
            sync.downloadPackage(packagePath, packageFile, retry)

            if (extract) {
                val jcrRoot = File(aem.config.packageJcrRoot)
                aem.logger.lifecycle("Extracting package $packageFile to $jcrRoot")

                extractDownloadedPackage(packageFile, jcrRoot)
            }

            aem.notifier.notify("Package downloaded", packageFile.name)
        } finally {
            aem.logger.lifecycle("Cleaning downloaded package: $packageFile")

            sync.deletePackage(packagePath)
        }
    }

    private fun clean() {
        GFileUtils.cleanDirectory(temporaryDir)
    }

    private fun prepareShellPackage(): File {
        val zipResult = File(temporaryDir, "$PKG_CLASSIFIER_SHELL.zip")
        val vltDir = File(shellDir, PackagePlugin.VLT_PATH)
        val jcrRoot = File(shellDir, PackagePlugin.JCR_ROOT)
        vltDir.mkdirs()
        jcrRoot.mkdirs()

        filter.file.copyTo(File(vltDir, "filter.xml"))
        FileOperations.copyResources(PackagePlugin.VLT_PATH, vltDir, true)

        val fileProperties = PackageFileFilter.FILE_PROPERTIES + mapOf(
                "project.version" to "${project.version}-$PKG_CLASSIFIER"
        )
        FileOperations.amendFiles(vltDir, PackageFileFilter.EXPAND_FILES_DEFAULT) { file, content ->
            aem.props.expandPackage(content, fileProperties, file.absolutePath)
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
        const val PKG_CLASSIFIER = "download"

        const val PKG_CLASSIFIER_SHELL = "downloadShell"
    }

}