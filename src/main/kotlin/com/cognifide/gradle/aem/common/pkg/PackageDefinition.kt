package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.asset.AssetManager
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Package builder that could be used to compose CRX package in place.
 *
 * This is programmatic approach to create ZIP file. API reflects Gradle's AbstractArchiveTask.
 * Useful for writing complex custom tasks that cannot inherit from Gradle's ZIP task.
 *
 * @see <https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Zip.html#org.gradle.api.tasks.bundling.Zip>
 */
class PackageDefinition(private val aem: AemExtension) : VaultDefinition(aem) {

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
        val pkgFile = File(pkgDir, "${Package.JCR_ROOT}$path")
        pkgFile.parentFile.mkdirs()
        FileUtils.copyFile(file, pkgFile)
    }

    fun copyMetaFiles(skipExisting: Boolean = true) {
        aem.assetManager.copyDir(AssetManager.META_RESOURCES_PATH, metaDir, !skipExisting)
    }

    fun expandMetaFiles(filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        expandFiles(metaDir, filePatterns)
    }

    val expandProperties = aem.obj.map<String, Any> { convention(aem.obj.provider { fileProperties }) }

    fun expandFiles(dir: File, filePatterns: List<String> = PackageFileFilter.EXPAND_FILES_DEFAULT) {
        FileOperations.amendFiles(dir, filePatterns) { source, content ->
            common.prop.expand(content, expandProperties.get(), source.absolutePath)
        }
    }

    /**
     * Compose a CRX package basing on configured definition.
     */
    fun compose(): File {
        archivePath.get().asFile.delete()
        pkgDir.deleteRecursively()
        metaDir.mkdirs()
        jcrDir.mkdirs()

        copyMetaFiles()
        content()
        expandMetaFiles()

        ZipFile(archivePath.get().asFile).packAll(pkgDir)
        pkgDir.deleteRecursively()

        return archivePath.get().asFile
    }
}
