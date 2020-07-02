package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import java.io.File

class QuickstartResolver(private val aem: AemExtension) {

    private val common = aem.common

    /**
     * Directory storing downloaded AEM Quickstart source files (JAR & license).
     */
    val downloadDir = aem.obj.dir {
        convention(aem.obj.buildDir(TEMPORARY_DIR))
        aem.prop.file("localInstance.quickstart.downloadDir")?.let { set(it) }
    }

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    val jarUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.jarUrl")?.let { set(it) }
    }

    val jar: File? get() = jarUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    /**
     * URI pointing to AEM quickstart license file.
     */
    val licenseUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.licenseUrl")?.let { set(it) }
    }

    val license: File? get() = licenseUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    val files: List<File> get() = listOfNotNull(jar, license)

    companion object {

        const val TEMPORARY_DIR = "instance/quickstart"
    }
}
