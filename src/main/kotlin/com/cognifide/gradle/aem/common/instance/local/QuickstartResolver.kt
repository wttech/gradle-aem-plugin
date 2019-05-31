package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class QuickstartResolver(aem: AemExtension) {

    private val downloadDir = AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    private val fileResolver = FileResolver(aem, downloadDir).apply { group(GROUP_EXTRA) {} }

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    var jarUrl = aem.props.string("localInstance.quickstart.jarUrl")

    @get:JsonIgnore
    val jar: File?
        get() = jarUrl?.run { fileResolver.download(this) }?.file

    /**
     * URI pointing to AEM quickstart license file.
     */
    var licenseUrl = aem.props.string("localInstance.quickstart.licenseUrl")

    @get:JsonIgnore
    val license: File?
        get() = licenseUrl?.run { fileResolver.download(this) }?.file

    @get:JsonIgnore
    val allFiles: List<File>
        get() = mandatoryFiles + extraFiles

    @get:JsonIgnore
    val mandatoryFiles: List<File>
        get() = listOfNotNull(jar, license)

    @get:JsonIgnore
    val extraFiles: List<File>
        get() = fileResolver.group(GROUP_EXTRA).files

    fun extraFiles(configurer: Resolver<FileGroup>.() -> Unit) {
        fileResolver.group(GROUP_EXTRA, configurer)
    }

    companion object {

        const val GROUP_EXTRA = "extra"

        const val TEMPORARY_DIR = "instance"
    }
}