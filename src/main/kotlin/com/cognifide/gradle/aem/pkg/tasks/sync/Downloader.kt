package com.cognifide.gradle.aem.pkg.tasks.sync

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.common.utils.using
import org.gradle.api.tasks.Internal
import java.io.File

class Downloader(@Internal private val aem: AemExtension) {

    private val logger = aem.logger

    /**
     * Determines instance from which JCR content will be downloaded.
     */
    val instance = aem.obj.typed<Instance> { convention(aem.obj.provider { aem.anyInstance }) }

    /**
     * Determines VLT filter used to grab JCR content from AEM instance.
     */
    val filter = aem.obj.typed<FilterFile> { convention(aem.obj.provider { aem.filter }) }

    /**
     * Allows to configure downloaded package details.
     */
    val definition = PackageDefinition(aem).apply {
        filterElements.set(filter.map { it.elements })
    }

    fun definition(options: PackageDefinition.() -> Unit) = definition.using(options)

    /**
     * Allows to delete existing contents before extracting downloaded one.
     */
    val clean = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("package.sync.downloader.clean")?.let { set(it) }
    }

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

    /**
     * Delete downloaded package after extraction.
     */
    val delete = aem.obj.boolean {
        convention(extract)
        aem.prop.boolean("package.sync.downloader.delete")?.let { set(it) }
    }

    fun download() {
        val file = instance.get().sync { packageManager.download(definition) }
        if (extract.get()) {
            extractDir.get().asFile.using {
                logger.info("Extracting package $file to $this")
                extractDownloadedPackage(file, this)
                if (delete.get()) {
                    logger.info("Deleting extracted package $file")
                    file.delete()
                }
            }
        }
    }

    private fun extractDownloadedPackage(downloadedPackage: File, jcrRoot: File) {
        jcrRoot.apply {
            if (exists() && clean.get()) {
                deleteRecursively()
            }
            mkdirs()
        }

        aem.project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                .from(aem.project.zipTree(downloadedPackage.path))
                .include("${Package.JCR_ROOT}/**")
        }
    }
}
