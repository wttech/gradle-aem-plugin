package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import java.io.File

class LocalHandleOptions(aem: AemExtension, downloadDir: File) {

    private val fileResolver = FileResolver(aem, downloadDir)

    var zipUrl = aem.props.string("aem.create.zipUrl")

    var jarUrl = aem.props.string("aem.create.jarUrl")

    var licenseUrl = aem.props.string("aem.create.licenseUrl")

    val zip: File?
        get() = zipUrl?.run { fileResolver.url(this).file }

    val jar: File?
        get() = jarUrl?.run { fileResolver.url(this).file }

    val license: File?
        get() = licenseUrl?.run { fileResolver.url(this).file }

    val allFiles: List<File>
        get() = mandatoryFiles + extraFiles

    val mandatoryFiles: List<File> = listOfNotNull(jar, license)

    val extraFiles: List<File>
        get() = fileResolver.group(GROUP_EXTRA).files

    fun extraFiles(configurer: Resolver<FileGroup>.() -> Unit) {
        fileResolver.group(GROUP_EXTRA, configurer)
    }

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    var overridesPath: String = "${aem.project.rootProject.file("src/main/resources/${InstancePlugin.FILES_PATH}")}"

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    var expandFiles: List<String> = listOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml", "**/start", "**/stop")

    /**
     * Custom properties that can be injected into instance files.
     */
    var expandProperties: Map<String, Any> = mapOf()

    companion object {
        const val GROUP_EXTRA = "extra"
    }
}