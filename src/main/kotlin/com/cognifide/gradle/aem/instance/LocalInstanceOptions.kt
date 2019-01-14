package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import java.io.File

class LocalInstanceOptions(aem: AemExtension, downloadDir: File) {

    private val fileResolver = FileResolver(aem, downloadDir)

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    var source = Source.of(aem.props.string("aem.localInstance.source") ?: Source.AUTO.name)

    /**
     * URI pointing to ZIP file created by backup task (packed AEM instances already created).
     */
    var zipUrl = aem.props.string("aem.localInstance.zipUrl")

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    var jarUrl = aem.props.string("aem.localInstance.jarUrl")

    /**
     * URI pointing to AEM quickstart license file.
     */
    var licenseUrl = aem.props.string("aem.localInstance.licenseUrl")

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

    enum class Source {
        AUTO,
        EXTERNAL,
        INTERNAL;

        companion object {
            fun of(name: String): Source {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported local instance source: $name")
            }
        }
    }

    companion object {
        const val GROUP_EXTRA = "extra"
    }
}