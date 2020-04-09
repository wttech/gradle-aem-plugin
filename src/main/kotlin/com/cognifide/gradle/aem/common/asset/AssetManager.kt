package com.cognifide.gradle.aem.common.asset

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import java.io.File

class AssetManager(private val aem: AemExtension) {

    val rootDir = aem.obj.dir { convention(aem.project.rootProject.layout.buildDirectory.dir("aem")) }

    private val assets get() = ZipFile(rootDir.file(ZIP_PATH).get().asFile.also { assetsFromResources(it) })

    private fun assetsFromResources(zip: File) = zip.apply {
        if (exists()) {
            return@apply
        }
        parentFile.mkdirs()
        outputStream().use { output ->
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

    companion object {
        const val ZIP_PATH = "assets.zip"

        const val META_PATH = "package/defaults/${Package.META_PATH}"

        const val OAKPAL_INITIAL = "package/validator/initial"

        const val OAKPAL_OPEAR_PATH = "package/validator/${Package.OAKPAL_OPEAR_PATH}"
    }
}
