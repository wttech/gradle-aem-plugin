package com.cognifide.gradle.aem.pkg.resolver

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.instance.Bundle
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.pkg.PackageException
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.util.GFileUtils

class PackageResolution(group: PackageGroup, id: String, action: (FileResolution) -> File) : FileResolution(group, id, action) {

    private val resolver = group.resolver

    private val aem = resolver.aem

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
        val pkg = File(dir, "$pkgName.zip")
        if (pkg.exists()) {
            aem.logger.info("CRX package wrapping OSGi bundle already exists: $pkg")
            return pkg
        }

        aem.logger.info("Wrapping OSGi bundle to CRX package: $jar")

        val bundle = Jar(jar)
        val bundlePath = "${resolver.bundlePath}/${jar.name}"
        val description = bundle.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_DESCRIPTION) ?: ""
        val symbolicName = bundle.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_SYMBOLIC_NAME)
        val group = symbolicName.substringBeforeLast(".")
        val version = bundle.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_VERSION)

        return aem.packageComposer.compose {
            this.file = pkg
            this.description = description

            this.group = group
            this.name = symbolicName
            this.version = version

            content { pkgRoot ->
                val pkgJar = File(pkgRoot, "${Package.JCR_ROOT}$bundlePath")
                GFileUtils.mkdirs(pkgJar.parentFile)
                FileUtils.copyFile(jar, pkgJar)
            }

            filter(bundlePath)

            resolver.bundleDefinition(this, bundle)
        }
    }
}