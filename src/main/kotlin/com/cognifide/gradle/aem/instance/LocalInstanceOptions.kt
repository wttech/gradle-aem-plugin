package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable

class LocalInstanceOptions(aem: AemExtension) : Serializable {

    private val downloadDir = AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    private val fileResolver = FileResolver(aem, downloadDir).apply { group(GROUP_EXTRA) {} }

    /**
     * Path in which local AEM instances will be stored.
     */
    var rootDir: File = aem.props.string("localInstance.root")?.let { aem.project.file(it) }
            ?: aem.projectMain.file(".aem/instance")

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    var jarUrl = aem.props.string("localInstance.jarUrl")

    /**
     * URI pointing to AEM quickstart license file.
     */
    var licenseUrl = aem.props.string("localInstance.licenseUrl")

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    var overridesPath: String = "${aem.configCommonDir}/${InstancePlugin.FILES_PATH}"

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    var expandFiles: List<String> = listOf(
            "**/start.bat",
            "**/stop.bat",
            "**/start",
            "**/stop"
    )

    /**
     * Custom properties that can be injected into instance files.
     */
    var expandProperties: Map<String, Any> = mapOf()

    @JsonIgnore
    var jarSource: FileResolver.() -> FileResolution? = {
        jarUrl?.run { url(this) }
    }

    @get:JsonIgnore
    val jar: File?
        get() = fileResolver.run(jarSource)?.file

    @JsonIgnore
    var licenseSource: FileResolver.() -> FileResolution? = {
        licenseUrl?.run { url(this) }
    }

    @get:JsonIgnore
    val license: File?
        get() = fileResolver.run(licenseSource)?.file

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