package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class QuickstartResolver(private val aem: AemExtension) {

    /**
     * Directory storing downloaded AEM Quickstart source files (JAR & license).
     */
    var downloadDir = aem.props.string("localInstance.quickstart.downloadDir")?.let { aem.project.file(it) }
            ?: AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    var jarUrl = aem.props.string("localInstance.quickstart.jarUrl")

    @get:JsonIgnore
    val jar: File?
        get() = jarUrl?.run { aem.fileTransfer.downloadTo(this, downloadDir) }

    /**
     * URI pointing to AEM quickstart license file.
     */
    var licenseUrl = aem.props.string("localInstance.quickstart.licenseUrl")

    @get:JsonIgnore
    val license: File?
        get() = licenseUrl?.run { aem.fileTransfer.downloadTo(this, downloadDir) }

    @get:JsonIgnore
    val files: List<File>
        get() = listOfNotNull(jar, license)

    companion object {

        const val TEMPORARY_DIR = "instance/quickstart"
    }
}