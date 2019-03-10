package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.pkg.vlt.VltDefinition
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil

/**
 * Package definition that could be used to compose CRX package in place.
 *
 * This is programmatic approach to create ZIP file.
 * Useful for writing complex custom tasks that cannot inherit from Gradle's ZIP task.
 */
class PackageDefinition(aem: AemExtension) : VltDefinition(aem) {

    private var fileCustom: File? = null

    /**
     * ZIP file being composed.
     */
    var file: File
        get() = fileCustom ?: File(aem.temporaryDir, "$group-$name-$version.zip")
        set(value) {
            fileCustom = value
        }

    /**
     * Temporary directory being zipped to produce CRX package.
     */
    val dir: File
        get() = File(file.parentFile, file.nameWithoutExtension)

    val metaDir: File
        get() = File(dir, Package.META_PATH)

    val jcrDir: File
        get() = File(dir, Package.JCR_ROOT)

    /**
     * Hook for customizing how package will be processed before zipping.
     */
    private var process: PackageDefinition.() -> Unit = {
        copyMetaFiles()
        expandMetaFiles()
    }

    fun process(options: PackageDefinition.() -> Unit) {
        this.process = options
    }

    /**
     * Hook for adding files to package being composed.
     */
    private var content: PackageDefinition.() -> Unit = {}

    fun content(options: PackageDefinition.() -> Unit) {
        this.content = options
    }

    /**
     * Compose a CRX package basing on configured definition.
     */
    fun compose(definition: PackageDefinition.() -> Unit): File {
        PackageDefinition(aem).apply {
            definition()
            ensureDefaults()

            file.delete()
            dir.deleteRecursively()
            metaDir.mkdirs()
            jcrDir.mkdirs()

            content()
            process()

            ZipUtil.pack(dir, file)
            dir.deleteRecursively()

            return file
        }
    }

    // 'content' & 'process' methods DSL

    fun copyJcrFile(file: File, path: String) {
        val pkgFile = File(dir, "${Package.JCR_ROOT}$path")
        GFileUtils.mkdirs(pkgFile.parentFile)
        FileUtils.copyFile(file, pkgFile)
    }

    fun copyMetaFiles() {
        FileOperations.copyResources(Package.META_PATH, metaDir, true)
    }

    fun expandMetaFiles() {
        expandFiles(metaDir, PackageFileFilter.EXPAND_FILES_DEFAULT)
    }

    fun expandFiles(dir: File, filePatterns: List<String>) {
        FileOperations.amendFiles(dir, filePatterns) { source, content ->
            aem.props.expandPackage(content, mapOf("definition" to this), source.absolutePath)
        }
    }
}