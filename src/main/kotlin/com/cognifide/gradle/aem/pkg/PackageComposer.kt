package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import java.io.File
import org.zeroturnaround.zip.ZipUtil

class PackageComposer(val aem: AemExtension) {

    fun compose(definition: PackageDefinition.() -> Unit): File {
        val pkgDefinition = PackageDefinition(aem).apply(definition)
        val pkgFile = pkgDefinition.file

        pkgFile.delete()

        val pkgDir = pkgDefinition.dir
        pkgDir.deleteRecursively()

        val vltDir = File(pkgDir, Package.VLT_PATH)
        vltDir.mkdirs()

        val jcrDir = File(pkgDir, Package.JCR_ROOT)
        jcrDir.mkdirs()

        FileOperations.copyResources(Package.VLT_PATH, vltDir)
        FileOperations.amendFiles(vltDir, PackageFileFilter.EXPAND_FILES_DEFAULT) { source, content ->
            aem.props.expandPackage(content, mapOf("definition" to definition), source.absolutePath)
        }

        pkgDefinition.contentCallbacks.forEach { it(pkgDir) }

        ZipUtil.pack(pkgDir, pkgFile)
        pkgDir.deleteRecursively()

        return pkgFile
    }
}