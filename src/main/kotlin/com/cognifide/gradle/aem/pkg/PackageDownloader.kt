package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.pkg.vlt.VltFilter
import java.io.File
import org.gradle.api.tasks.Internal

class PackageDownloader(@Internal private val aem: AemExtension) {

    var instance: Instance = aem.anyInstance

    var filter: VltFilter = aem.filter

    /**
     * Repeat download when failed (brute-forcing).
     */
    var retry = aem.retry { afterSquaredSecond(aem.props.long("packageDownload.retry") ?: 3) }

    /**
     * Extract the contents of download package to current project 'jcr_root' directory.
     *
     * This operation can be modified using -Pforce command line to replace the contents of jcr_root directory
     * with package content.
     */
    var extract = aem.props.boolean("packageDownload.extract") ?: true

    /**
     * In case of downloading big CRX packages, AEM could respond much slower so that special
     * timeout is covering such edge case.
     */
    var httpOptions: HttpClient.() -> Unit = {
        connectionTimeout = aem.props.int("packageDownload.httpOptions.connectionTimeout") ?: 60000
    }

    fun download() {
        val file = instance.sync.apply(httpOptions).downloadPackage({
            filterElements = filter.rootElements.toMutableList()
        }, retry)

        if (extract) {
            val jcrRoot = File(aem.config.packageJcrRoot)
            aem.logger.lifecycle("Extracting package $file to $jcrRoot")

            extractDownloadedPackage(file, jcrRoot)
        }
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

        const val PKG_VERSION = "download"
    }
}