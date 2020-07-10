package com.cognifide.gradle.sling.pkg.tasks.sync

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.pkg.PackageDefinition
import com.cognifide.gradle.sling.common.pkg.vault.FilterFile
import com.cognifide.gradle.common.utils.using
import java.io.File
import org.gradle.api.tasks.Internal

class Downloader(@Internal private val sling: SlingExtension) {

    private val common = sling.common

    /**
     * Determines instance from which JCR content will be downloaded.
     */
    val instance = sling.obj.typed<Instance> { convention(sling.obj.provider { sling.anyInstance }) }

    /**
     * Determines VLT filter used to grab JCR content from Sling instance.
     */
    val filter = sling.obj.typed<FilterFile> { convention(sling.obj.provider { sling.filter }) }

    /**
     * Allows to configure downloaded package details.
     */
    val definition = PackageDefinition(sling).apply {
        filterElements.set(filter.map { it.elements })
    }

    fun definition(options: PackageDefinition.() -> Unit) = definition.using(options)

    /**
     * Allows to delete existing contents before extracting downloaded one.
     */
    val clean = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("package.sync.downloader.clean")?.let { set(it) }
    }

    /**
     * Allows to disable extracting contents of download package to directory.
     *
     * This operation can be modified using '-Pforce' command line to replace the contents of extract directory
     * with package content.
     */
    val extract = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.sync.downloader.extract")?.let { set(it) }
    }

    /**
     * Path in which downloader JCR content will be extracted.
     */
    val extractDir = sling.obj.dir {
        convention(sling.packageOptions.jcrRootDir)
        sling.prop.file("package.sync.downloader.extractDir")?.let { set(it) }
    }

    fun download() {
        val file = instance.get().sync { packageManager.download(definition) }
        if (extract.get()) {
            extractDir.get().asFile.using {
                sling.logger.info("Extracting package $file to $this")
                extractDownloadedPackage(file, this)
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

        sling.project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(sling.project.zipTree(downloadedPackage.path))
                    .include("${Package.JCR_ROOT}/**")
        }
    }
}
