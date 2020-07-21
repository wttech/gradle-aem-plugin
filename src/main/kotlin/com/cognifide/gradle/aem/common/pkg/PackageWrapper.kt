package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.bundle.BundleFile
import java.io.File

class PackageWrapper(val aem: AemExtension) {

    val workDir = aem.obj.dir { convention(aem.obj.buildDir("package/wrapper")) }

    val bundlePath = aem.obj.string { convention("/apps/gap/wrapper/install") }

    fun definition(definition: PackageDefinition.(BundleFile) -> Unit) {
        this.definition = definition
    }

    private var definition: PackageDefinition.(BundleFile) -> Unit = {}

    fun wrap(file: File): File = when (file.extension) {
        "jar" -> wrapJar(file)
        "zip" -> file
        else -> throw PackageException("File '$file' must have '*.jar' or '*.zip' extension")
    }

    fun wrapJar(jar: File): File {
        val pkgName = jar.nameWithoutExtension
        val pkg = File(workDir.get().asFile, "$pkgName.zip")
        if (pkg.exists()) {
            aem.logger.info("CRX package wrapping OSGi bundle already exists: $pkg")
            return pkg
        }

        aem.logger.info("Wrapping OSGi bundle to CRX package: $jar")

        val bundle = BundleFile(jar)
        val bundlePath = "${bundlePath.get()}/${jar.name}"

        return aem.composePackage {
            this.archivePath.set(pkg)
            this.description.set(bundle.description)
            this.group.set(bundle.group)
            this.name.set(bundle.symbolicName)
            this.version.set(bundle.version)

            filter(bundlePath)
            content { copyJcrFile(jar, bundlePath) }

            definition(bundle)
        }
    }
}
