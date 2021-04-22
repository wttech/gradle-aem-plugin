package com.cognifide.gradle.aem.common.asset

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.zip.ZipFile
import java.io.File
import java.io.InputStream

class AssetManager(private val aem: AemExtension) {

    val rootDir = aem.obj.dir { convention(aem.project.rootProject.layout.projectDirectory.dir(".gradle/aem/plugin/${AemPlugin.BUILD.pluginVersion}")) }

    private val assets get() = ZipFile(rootDir.file(ZIP_PATH).get().asFile.also { assetsFromResources(it) })

    private fun assetsFromResources(zip: File) = aem.common.buildScope.doOnce("extract assets to zip '$zip'") {
        if (zip.exists()) {
            return@doOnce
        }
        zip.parentFile.mkdirs()
        zip.outputStream().use { output ->
            this@AssetManager.javaClass.getResourceAsStream("/$ZIP_PATH").use {
                input -> input.copyTo(output)
            }
        }
    }

    fun readFile(path: String) = assets.readFile(path)

    fun copyFile(path: String, targetFile: File, override: Boolean = true) = copyFileInternal(path, targetFile, override)

    fun copyDir(path: String, targetDir: File, override: Boolean = true) = assets.walkDir(path) { fileHeader ->
        if (!fileHeader.isDirectory) {
            val targetRelativePath = fileHeader.fileName.removePrefix(path).removePrefix("/")
            copyFileInternal(fileHeader.fileName, targetDir.resolve(targetRelativePath), override)
        }
    }

    private fun copyFileInternal(path: String, targetFile: File, override: Boolean) {
        if (override) {
            if (targetFile.exists()) targetFile.delete()
            targetFile.parentFile.mkdirs()
            assets.unpackFile(path, targetFile)
        } else if (!targetFile.exists()) {
            targetFile.parentFile.mkdirs()
            assets.unpackFile(path, targetFile)
        }
    }

    /**
     * Find files in the given directory path and prepare input streams and new file paths in the given
     * target directory
     */
    fun getStreams(path: String, targetPath: String): Map<String, () -> InputStream> {
        val entryStreams = mutableMapOf<String, () -> InputStream>()
        assets.walkDir(path) { fileHeader ->
            if (!fileHeader.isDirectory) {
                val targetRelativePath = fileHeader.fileName.removePrefix(path)
                entryStreams[targetPath + targetRelativePath] = { assets.readFile(fileHeader.fileName) }
            }
        }
        return entryStreams
    }

    companion object {
        const val ZIP_PATH = "assets.zip"

        const val META_PATH = "package/defaults/${Package.META_PATH}"

        const val OAKPAL_INITIAL = "package/validator/initial"

        const val OAKPAL_OPEAR_PATH = "package/validator/${Package.OAKPAL_OPEAR_PATH}"
    }
}
