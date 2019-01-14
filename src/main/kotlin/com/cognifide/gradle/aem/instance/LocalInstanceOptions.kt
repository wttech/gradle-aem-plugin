package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import java.io.File

class LocalInstanceOptions(aem: AemExtension, downloadDir: File) {

    private val fileResolver = FileResolver(aem, downloadDir)

    /**
     * Determines how instances will be created (from backup or from the scratch).
     */
    var source = Source.of(aem.props.string("aem.localInstance.source") ?: Source.AUTO.name)

    /**
     * Defines backup selection rule.
     *
     * By default, it just takes most recent backup (file names sorted lexically / descending).
     */
    var zipSelector: Collection<File>.() -> File? = { sortedByDescending { it.name }.firstOrNull() }

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

    var zipSource: FileResolver.() -> FileResolution? = {
        zipUrl?.run { url(this) }
    }

    val zip: File?
        get() = fileResolver.run(zipSource)?.file

    var jarSource: FileResolver.() -> FileResolution? = {
        jarUrl?.run { url(this) }
    }

    val jar: File?
        get() = fileResolver.run(jarSource)?.file

    var licenseSource: FileResolver.() -> FileResolution? = {
        licenseUrl?.run { url(this) }
    }

    val license: File?
        get() = fileResolver.run(licenseSource)?.file

    val allFiles: List<File>
        get() = mandatoryFiles + extraFiles

    val mandatoryFiles: List<File>
        get() = listOfNotNull(jar, license)

    val extraFiles: List<File>
        get() = fileResolver.group(GROUP_EXTRA).files

    fun extraFiles(configurer: Resolver<FileGroup>.() -> Unit) {
        fileResolver.group(GROUP_EXTRA, configurer)
    }

    enum class Source {
        /**
         * Create instances from most recent backup (external or internal)
         * or fallback to creating from the scratch if there is no backup available.
         */
        AUTO,
        /**
         * Force creating instances from the scratch.
         */
        NONE,
        /**
         * Force using backup available at external source (specified in 'aem.localInstance.zipUrl').
         */
        BACKUP_EXTERNAL,
        /**
         * Force using internal backup (created by task 'aemBackup').
         */
        BACKUP_INTERNAL;

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