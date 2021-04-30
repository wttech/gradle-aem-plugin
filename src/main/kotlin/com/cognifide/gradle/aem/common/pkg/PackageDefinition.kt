package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.asset.AssetManager
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.zip.ZipFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader

/**
 * Package builder that could be used to compose CRX package in place.
 *
 * This is programmatic approach to create ZIP file. API reflects Gradle's AbstractArchiveTask.
 * Useful for writing complex custom tasks that cannot inherit from Gradle's ZIP task.
 *
 * @see <https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Zip.html#org.gradle.api.tasks.bundling.Zip>
 */
class PackageDefinition(private val aem: AemExtension) : VaultDefinition(aem) {

    private val streamsToZip = mutableMapOf<String, () -> InputStream>()

    private val filesToZip = mutableMapOf<String, File>()

    val destinationDirectory = aem.obj.buildDir("package")

    val archiveBaseName = aem.obj.string { convention(aem.commonOptions.baseName) }

    val archiveAppendix = aem.obj.string()

    val archiveExtension = aem.obj.string { convention("zip") }

    val archiveClassifier = aem.obj.string()

    val archiveVersion = aem.obj.string { convention(version) }

    /**
     * ZIP file path
     */
    val archivePath = aem.obj.file {
        convention(aem.obj.provider { destinationDirectory.file(archiveFileName).get() })
    }

    /**
     * ZIP file name
     */
    val archiveFileName = aem.obj.string {
        convention(aem.obj.provider {
            listOf(archiveBaseName.orNull, archiveAppendix.orNull, archiveVersion.orNull, archiveClassifier.orNull)
                    .filter { !it.isNullOrBlank() }
                    .joinToString("-")
                    .run { "$this.${archiveExtension.get()}" }
        })
    }

    /**
     * Temporary directory being zipped to produce CRX package.
     */
    //TODO remove
    val pkgDir: File get() = archivePath.get().asFile.parentFile
            .resolve("${archivePath.get().asFile.nameWithoutExtension}.pkg")

    val metaDir: File get() = pkgDir.resolve(Package.META_PATH)

    val jcrDir: File get() = pkgDir.resolve(Package.JCR_ROOT)

    /**
     * Hook for adding files to package being composed.
     */
    fun content(options: PackageDefinition.() -> Unit) {
        this.content = options
    }

    private var content: PackageDefinition.() -> Unit = {}

    // 'content' & 'process' methods DSL

    fun copyJcrFile(file: File, path: String) {
        val fullPath = "${Package.JCR_ROOT}$path"
        filesToZip[fullPath] = file
    }

    /**
     * Remember files to add to a zip file
     */
    private fun copyMetaFiles() {
        val metaFiles = aem.assetManager.getStreams(AssetManager.META_PATH, Package.META_PATH)
            //skip existing
            .filterKeys { path -> !streamsToZip.contains(path) && !filesToZip.contains(path) }
        this.streamsToZip.putAll(metaFiles)
    }

    /**
     * Add expand amenders to files that are to be added to a zip file
     */
    private fun expandMetaFiles(filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        val metaFiles = streamsToZip
            .filter { it.key.startsWith(Package.META_PATH) }
            .filter  { Patterns.wildcard(it.key, filePatterns) }
            .mapValues { expandInputStream(it.value, it.key)}
        streamsToZip.putAll(metaFiles)
    }

    /**
     * Expand template variables in an input stream
     */
    private fun expandInputStream(inputProvider: () -> InputStream, path: String): () -> InputStream {
        return {
            val content = inputProvider.invoke().bufferedReader().use { it.readText() }
            val result = common.prop.expand(content, expandProperties.get(), path)
            ByteArrayInputStream(result.toByteArray())
        }
    }

    val expandProperties = aem.obj.map<String, Any> { convention(aem.obj.provider { fileProperties }) }

    /**
     * Expand template variables in files
     */
    private fun expandFiles(dir: File, filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        FileOperations.amendFiles(dir, filePatterns) { source, content ->
            common.prop.expand(content, expandProperties.get(), source.absolutePath)
        }
    }

    /**
     * Compose a CRX package basing on configured definition.
     */
    fun compose(): File {
        archivePath.get().asFile.delete()
        metaDir.mkdirs()
        jcrDir.mkdirs()

        copyMetaFiles()
        content()
        expandMetaFiles()

        val zipFile = ZipFile(archivePath.get().asFile)
        filesToZip.forEach { name, file -> zipFile.packAll(file) { fileNameInZip = name } }

        streamsToZip.forEach { name, streamProvider -> zipFile.packStream(streamProvider.invoke(), options = {fileNameInZip = name}) }

        return archivePath.get().asFile
    }
}
