package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import java.io.File
import org.zeroturnaround.zip.ZipUtil

class PackageComposer(val aem: AemExtension) {

    fun compose(definition: PackageDefinition.() -> Unit): File {
        val pkgDefinition = PackageDefinition(aem).apply(definition)
        val pkgName = "${pkgDefinition.group}-${pkgDefinition.name}-${pkgDefinition.version}.zip"
        val pkgFile = File(aem.temporaryDir, pkgName)

        return compose(pkgFile, pkgDefinition)
    }

    fun compose(file: File, definition: PackageDefinition.() -> Unit): File {
        return compose(file, PackageDefinition(aem).apply(definition))
    }

    private fun compose(file: File, definition: PackageDefinition): File {
        val pkgDir = File(aem.temporaryDir, "${file.name}_package")
        val vltDir = File(pkgDir, Package.VLT_PATH)
        val jcrDir = File(pkgDir, Package.JCR_ROOT)

        file.delete()
        pkgDir.deleteRecursively()

        vltDir.mkdirs()
        jcrDir.mkdirs()

        FileOperations.copyResources(Package.VLT_PATH, vltDir)
        FileOperations.amendFiles(vltDir, PackageFileFilter.EXPAND_FILES_DEFAULT) { source, content ->
            aem.props.expandPackage(content, mapOf("definition" to definition), source.absolutePath)
        }

        definition.contentCallbacks.forEach { it(pkgDir) }

        ZipUtil.pack(pkgDir, file)
        pkgDir.deleteRecursively()

        return file
    }
}