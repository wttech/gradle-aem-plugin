package com.cognifide.gradle.aem.tooling.sync

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.VltFilter
import java.io.File
import org.gradle.api.tasks.Internal

class Downloader(@Internal private val aem: AemExtension) {

    /**
     * Determines instance from which JCR content will be downloaded.
     */
    var instance: Instance = aem.anyInstance

    /**
     * Determines VLT filter used to grab JCR content from AEM instance.
     */
    var filter: VltFilter = aem.filter

    /**
     * Allows to disable extracting contents of download package to directory.
     *
     * This operation can be modified using '-Pforce' command line to replace the contents of extract directory
     * with package content.
     */
    var extract = aem.props.boolean("sync.downloader.extract") ?: true

    /**
     * Path in which downloader JCR content will be extracted.
     */
    var extractDir: File = aem.props.string("sync.downloader.extractDir")?.let { aem.project.file(it) }
            ?: aem.packageOptions.jcrRootDir

    /**
     * Repeat download when failed (brute-forcing).
     */
    var retry = aem.retry { afterSquaredSecond(aem.props.long("sync.downloader.retry") ?: 3) }

    fun download() {
        val file = instance.sync.packageManager.downloadPackage({
            filterElements = filter.rootElements.toMutableList()
        }, retry)

        if (extract) {
            aem.logger.lifecycle("Extracting package $file to $extractDir")
            extractDownloadedPackage(file, extractDir)
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
}