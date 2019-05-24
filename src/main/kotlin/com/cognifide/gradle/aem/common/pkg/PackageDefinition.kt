package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.VltDefinition
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil

/**
 * Package definition that could be used to compose CRX package in place.
 *
 * This is programmatic approach to create ZIP file. API reflects Gradle's AbstractArchiveTask.
 * Useful for writing complex custom tasks that cannot inherit from Gradle's ZIP task.
 */
class PackageDefinition(private val aem: AemExtension) : VltDefinition(aem) {

    var destinationDir: File = aem.temporaryDir

    var baseName: String = aem.baseName

    var appendix: String? = null

    var extension: String = "zip"

    var classifier: String? = null

    /**
     * ZIP file path
     */
    var archivePath: File
        set(value) {
            archivePathCustom = value
        }
        get() = archivePathCustom ?: File(destinationDir, archiveName)

    private var archivePathCustom: File? = null

    val archiveBaseName: String
        get() = listOf(baseName, appendix, version, classifier)
                .filter { !it.isNullOrBlank() }.joinToString("-")

    /**
     * ZIP file name
     */
    var archiveName: String
        set(value) {
            archiveNameCustom = value
        }
        get() = archiveNameCustom ?: "$archiveBaseName.$extension"

    private var archiveNameCustom: String? = null

    /**
     * Temporary directory being zipped to produce CRX package.
     */
    val pkgDir: File
        get() = File(archivePath.parentFile, archivePath.nameWithoutExtension)

    val metaDir: File
        get() = File(pkgDir, Package.META_PATH)

    val jcrDir: File
        get() = File(pkgDir, Package.JCR_ROOT)

    private var process: PackageDefinition.() -> Unit = {
        copyMetaFiles()
        expandMetaFiles()
    }

    /**
     * Hook for customizing how package will be processed before zipping.
     */
    fun process(options: PackageDefinition.() -> Unit) {
        this.process = options
    }

    private var content: PackageDefinition.() -> Unit = {}

    /**
     * Hook for adding files to package being composed.
     */
    fun content(options: PackageDefinition.() -> Unit) {
        this.content = options
    }

    // 'content' & 'process' methods DSL

    fun copyJcrFile(file: File, path: String) {
        val pkgFile = File(pkgDir, "${Package.JCR_ROOT}$path")
        GFileUtils.mkdirs(pkgFile.parentFile)
        FileUtils.copyFile(file, pkgFile)
    }

    fun copyMetaFiles(skipExisting: Boolean = true) {
        FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, skipExisting)
    }

    fun expandMetaFiles(filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        expandFiles(metaDir, filePatterns)
    }

    fun expandFiles(dir: File, filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        FileOperations.amendFiles(dir, filePatterns) { source, content ->
            aem.props.expandPackage(content, mapOf("definition" to this), source.absolutePath)
        }
    }

    /**
     * Compose a CRX package basing on configured definition.
     */
    fun compose(definition: PackageDefinition.() -> Unit): File {
        definition()
        ensureDefaults()

        archivePath.delete()
        pkgDir.deleteRecursively()
        metaDir.mkdirs()
        jcrDir.mkdirs()

        content()
        process()

        ZipUtil.pack(pkgDir, archivePath)
        pkgDir.deleteRecursively()

        return archivePath
    }
}