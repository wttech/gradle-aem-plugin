package com.cognifide.gradle.aem.pkg.tasks.sync

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import java.io.File
import org.gradle.api.tasks.Internal

class Downloader(@Internal private val aem: AemExtension) {

    private val common = aem.common

    /**
     * Determines instance from which JCR content will be downloaded.
     */
    val instance = aem.obj.typed<Instance> { convention(aem.obj.provider { aem.anyInstance }) }

    /**
     * Determines VLT filter used to grab JCR content from AEM instance.
     */
    val filter = aem.obj.typed<FilterFile> { convention(aem.obj.provider { aem.filter }) }

    /**
     * Allows to disable extracting contents of download package to directory.
     *
     * This operation can be modified using '-Pforce' command line to replace the contents of extract directory
     * with package content.
     */
    val extract = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.sync.downloader.extract")?.let { set(it) }
    }

    /**
     * Path in which downloader JCR content will be extracted.
     */
    val extractDir = aem.obj.dir {
        convention(aem.packageOptions.jcrRootDir)
        aem.prop.file("package.sync.downloader.extractDir")?.let { set(it) }
    }

    fun download() {
        val file = instance.get().sync.packageManager.download {
            filterElements = filter.get().elements.toMutableList()
        }

        if (extract.get()) {
            aem.logger.lifecycle("Extracting package $file to $extractDir")
            extractDownloadedPackage(file, extractDir.get().asFile)
        }
    }

    private fun extractDownloadedPackage(downloadedPackage: File, jcrRoot: File) {
        if (jcrRoot.exists() && common.prop.force) {
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
