package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.pkg.vlt.VltDefinition
import java.io.File
import org.zeroturnaround.zip.ZipUtil

class PackageComposer(val aem: AemExtension) {

    fun compose(definition: VltDefinition.() -> Unit) = compose(definition) {}

    fun compose(definition: VltDefinition.() -> Unit, amender: (File) -> Unit): File {
        val pkgDefinition = VltDefinition(aem).apply(definition)

        val pkgFile = File(aem.temporaryDir, "${pkgDefinition.group}-${pkgDefinition.name}-${pkgDefinition.version}.zip")
        val pkgDir = File(aem.temporaryDir, "${pkgFile.name}_package")
        val vltDir = File(pkgDir, Package.VLT_PATH)
        val jcrDir = File(pkgDir, Package.JCR_ROOT)

        pkgFile.delete()
        pkgDir.deleteRecursively()

        vltDir.mkdirs()
        jcrDir.mkdirs()

        FileOperations.copyResources(Package.VLT_PATH, vltDir)
        FileOperations.amendFiles(vltDir, PackageFileFilter.EXPAND_FILES_DEFAULT) { source, content ->
            aem.props.expandPackage(content, mapOf("definition" to pkgDefinition), source.absolutePath)
        }

        amender(pkgDir)

        ZipUtil.pack(pkgDir, pkgFile)
        pkgDir.deleteRecursively()

        return pkgFile
    }
}