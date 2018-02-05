package com.cognifide.gradle.aem.instance.satisfy

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.util.GFileUtils
import org.jsoup.nodes.Element
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class PackageResolution(group: PackageGroup, id: String, action: (FileResolution) -> File) : FileResolution(group, id, action) {

    val config = AemConfig.of(group.resolver.project)

    override fun process(file: File): File {
        val origin = super.process(file)

        return when (FilenameUtils.getExtension(file.name)) {
            "jar" -> wrap(origin)
            "zip" -> origin
            else -> throw PackageException("File $origin must have *.jar or *.zip extension")
        }
    }

    private fun wrap(jar: File): File {
        val pkgName = jar.nameWithoutExtension
        val pkg = File(jar.parentFile, "$pkgName.zip")
        val pkgRoot = File(jar.parentFile, pkgName)
        val pkgPath = "/apps/gep/satisfy/install/$jar"
        val vaultDir = File(pkgRoot, PackagePlugin.VLT_PATH)

        // Copy package template files
        GFileUtils.mkdirs(vaultDir)
        FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir)

        // Expand package properties
        val bundle = Jar(jar)
        val description = bundle.manifest.mainAttributes.getValue("Bundle-Description") ?: ""
        val symbolicName = bundle.manifest.mainAttributes.getValue("Bundle-SymbolicName")
        val group = symbolicName.substringBeforeLast(".")
        val version = bundle.manifest.mainAttributes.getValue("Bundle-Version")

        val props = mapOf<String, Any>(
                "project.group" to group,
                "project.name" to symbolicName,
                "project.version" to version,
                "project.description" to description,
                "config.packageName" to symbolicName,
                "config.acHandling" to config.acHandling,
                "filters" to listOf(Element("<filter root=\"$pkgPath\"/>"))
        ) + config.fileProperties

        FileOperations.amendFiles(vaultDir, config.filesExpanded, { file, line ->
            config.propParser.expandEnv(line, props, file.absolutePath)
        })

        // Copy bundle to install path
        val pkgJar = File("jcr_root$pkgPath")
        GFileUtils.mkdirs(pkgJar.parentFile)
        FileUtils.copyFile(pkgRoot, pkgJar)

        // ZIP all to CRX package
        ZipUtil.pack(pkgRoot, pkg)

        return pkg
    }
}