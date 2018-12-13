package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Collections
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.vlt.VltFilter
import java.io.File
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Internal
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil

class PackageDownloader(
    @Internal
    private val aem: AemExtension,
    @Internal
    private val temporaryDir: File
) {

    private val shellDir = File(temporaryDir, PKG_SHELL)

    var instance: Instance = aem.instanceAny

    var filter: VltFilter = aem.filter

    /**
     * Repeat download when failed (brute-forcing).
     */
    var retry = aem.retry { afterSquaredSecond(aem.props.long("aem.packageDownload.retry") ?: 3) }

    /**
     * Extract the contents of package downloaded using aemDownload task to current project jcr_root directory
     * This operation can be modified using -Paem.force command line to replace the contents of jcr_root directory with
     * package content
     */
    var extract = aem.props.boolean("aem.packageDownload.extract") ?: true

    /**
     * In case of downloading big CRX packages, AEM could respond much slower so that special
     * timeout is covering such edge case.
     */
    var httpOptions: HttpClient.() -> Unit = {
        connectionTimeout = aem.props.int("aem.packageDownload.httpOptions.connectionTimeout") ?: 60000
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
        } finally {
            aem.logger.lifecycle("Deleting downloaded package: $packageFile")

            sync.deletePackage(packagePath)
        }
    }

    private fun clean() {
        GFileUtils.cleanDirectory(temporaryDir)
    }

    private fun prepareShellPackage(): File {
        val zipResult = File(temporaryDir, "$PKG_SHELL.zip")
        val vltDir = File(shellDir, Package.VLT_PATH)
        val jcrRoot = File(shellDir, Package.JCR_ROOT)

        vltDir.mkdirs()
        jcrRoot.mkdirs()

        filter.file.copyTo(File(vltDir, VltFilter.BUILD_NAME))
        FileOperations.copyResources(Package.VLT_PATH, vltDir, true)

        val fileProperties = Collections.extendMap(PackageFileFilter.FILE_PROPERTIES, mapOf<String, Any>(
                "compose" to mapOf(
                        "vaultName" to aem.baseName,
                        "vaultGroup" to aem.project.group,
                        "vaultVersion" to PKG_VERSION

                ),
                "project" to mapOf(
                        "group" to aem.project.group,
                        "name" to aem.baseName,
                        "version" to PKG_VERSION,
                        "description" to aem.project.description.orEmpty()
                )
        ))
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

        aem.project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(aem.project.zipTree(downloadedPackage.path))
                    .include("${Package.JCR_ROOT}/**")
        }
    }

    companion object {
        const val PKG_SHELL = "shell"

        const val PKG_VERSION = "download"
    }
}