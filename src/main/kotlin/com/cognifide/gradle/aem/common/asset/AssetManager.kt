package com.cognifide.gradle.aem.common.asset

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import org.apache.commons.io.FileUtils
import org.gradle.api.provider.Provider
import java.io.File

class AssetManager(private val aem: AemExtension) {

    val rootDir = aem.obj.dir { convention(aem.project.rootProject.layout.buildDirectory.dir("aem/asset")) }

    fun file(path: String): Provider<File> {
        extractAll()
        return rootDir.file(path).map { it.asFile }
    }

    // TODO handle override false
    fun copyDir(path: String, targetDir: File, override: Boolean = true) {
        val sourceDir = file(path).get()
        FileUtils.copyDirectory(sourceDir, targetDir)
    }

    @Synchronized // TODO global lock via BuildScope / singleton
    private fun extractAll() {
        val dir = rootDir.get().asFile
        if (dir.exists()) {
            return
        }

        dir.mkdirs()
        val file = dir.resolve("assets.zip")
        file.outputStream().use { output -> javaClass.getResourceAsStream("/assets.zip").use { input -> input.copyTo(output) } }

        aem.project.zipTree(file).visit { it.copyTo(it.relativePath.getFile(dir)) } // TODO lingala unpackAll is broken here, report it
        file.deleteRecursively()
    }

    companion object {
        const val META_RESOURCES_PATH = "package/defaults/${Package.META_PATH}"

        const val OAKPAL_INITIAL = "package/validator/initial"

        const val OAKPAL_OPEAR_RESOURCES_PATH = "package/validator/${Package.OAKPAL_OPEAR_PATH}"
    }
}
