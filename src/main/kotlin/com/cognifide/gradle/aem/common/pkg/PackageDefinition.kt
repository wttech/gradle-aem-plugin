package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.VltDefinition
import org.apache.commons.io.FileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

/**
 * Package definition that could be used to compose CRX package in place.
 *
 * This is programmatic approach to create ZIP file. API reflects Gradle's AbstractArchiveTask.
 * Useful for writing complex custom tasks that cannot inherit from Gradle's ZIP task.
 *
 * @see <https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Zip.html#org.gradle.api.tasks.bundling.Zip>
 */
class PackageDefinition(private val aem: AemExtension) : VltDefinition(aem) {

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
        convention(destinationDirectory.file(archiveFileName))
    }

    /**
     * ZIP file name
     */
    val archiveFileName = aem.obj.string {
        convention(aem.obj.provider {
            listOf(archiveBaseName.get(), archiveAppendix.get(), archiveVersion.get(), archiveClassifier.get())
                    .filter { !it.isNullOrBlank() }
                    .joinToString("-")
                    .run { "$this.$archiveExtension" }
        })
    }

    /**
     * Temporary directory being zipped to produce CRX package.
     */
    val pkgDir: File get() = archivePath.get().asFile.parentFile.resolve(archivePath.get().asFile.nameWithoutExtension)

    val metaDir: File get() = pkgDir.resolve(Package.META_PATH)

    val jcrDir: File get() = pkgDir.resolve(Package.JCR_ROOT)

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
        pkgFile.parentFile.mkdirs()
        FileUtils.copyFile(file, pkgFile)
    }

    fun copyMetaFiles(skipExisting: Boolean = true) {
        FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, skipExisting)
    }

    fun expandMetaFiles(filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        expandFiles(metaDir, filePatterns)
    }

    val expandProperties: Map<String, Any> get() = mapOf(
            "definition" to this,
            "aem" to aem
    )

    fun expandFiles(dir: File, filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        FileOperations.amendFiles(dir, filePatterns) { source, content ->
            common.prop.expand(content, expandProperties, source.absolutePath)
        }
    }

    /**
     * Compose a CRX package basing on configured definition.
     */
    fun compose(definition: PackageDefinition.() -> Unit): File {
        definition()

        archivePath.get().asFile.delete()
        pkgDir.deleteRecursively()
        metaDir.mkdirs()
        jcrDir.mkdirs()

        content()
        process()

        ZipUtil.pack(pkgDir, archivePath.get().asFile)
        pkgDir.deleteRecursively()

        return archivePath.get().asFile
    }
}
