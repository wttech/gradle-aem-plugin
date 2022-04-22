package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import java.io.File

class QuickstartResolver(private val aem: AemExtension) {

    private val common = aem.common

    /**
     * Directory storing downloaded AEM Quickstart source files (JAR & license).
     */
    val downloadDir = aem.obj.dir {
        convention(aem.obj.buildDir("localInstance/quickstart"))
        aem.prop.file("localInstance.quickstart.downloadDir")?.let { set(it) }
    }

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    val jarUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.jarUrl")?.let { set(it) }
        aem.prop.string("localInstance.quickstart.dirUrl")?.let {
            set(
                File(it).walk()
                    .filter { ".*-quickstart-\\d\\.\\d\\.\\d+\\.jar".toRegex().matches(it.name) }
                    .first().absolutePath
            )
        }
    }

    val jar: File? get() = jarUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    /**
     * URI pointing to AEM quickstart license file.
     */
    val licenseUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.licenseUrl")?.let { set(it) }
        aem.prop.string("localInstance.quickstart.dirUrl")?.let {
            set(File(it).walk().filter { it.name.equals("license.properties") }.first().absolutePath)
        }
    }

    val license: File? get() = licenseUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    val files: List<File> get() = listOfNotNull(jar, license)

    /**
     * Shorthand for setting both requires URLs at once.
     */
    fun files(jarUrl: String, licenseUrl: String) {
        this.jarUrl.set(jarUrl)
        this.licenseUrl.set(licenseUrl)
    }
}
