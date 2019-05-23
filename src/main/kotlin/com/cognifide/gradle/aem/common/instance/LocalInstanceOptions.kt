package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileGroup
import com.cognifide.gradle.aem.common.file.resolver.FileResolution
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.file.resolver.Resolver
import com.cognifide.gradle.aem.instance.InstancePlugin
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
     * Determines how instances will be created (from backup or from the scratch).
     */
    var source = Source.of(aem.props.string("localInstance.source")
            ?: Source.AUTO.name)

    /**
     * Defines backup selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup
     * (file names sorted lexically / descending).
     */
    @JsonIgnore
    var zipSelector: Collection<File>.() -> File? = {
        val name = aem.props.string("localInstance.zipName") ?: ""
        when {
            name.isNotBlank() -> firstOrNull { it.name == name }
            else -> sortedByDescending { it.name }.firstOrNull()
        }
    }

    /**
     * URI pointing to ZIP file created by backup task (packed AEM instances already created).
     */
    var zipUrl = aem.props.string("localInstance.zipUrl")

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
    var overridesDir = File(aem.configCommonDir, InstancePlugin.FILES_PATH)

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
    var zipSource: FileResolver.() -> FileResolution? = {
        zipUrl?.run { url(this) }
    }

    @get:JsonIgnore
    val zip: File?
        get() = fileResolver.run(zipSource)?.file

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
         * Force using backup available at external source (specified in 'localInstance.zipUrl').
         */
        BACKUP_EXTERNAL,
        /**
         * Force using internal backup (created by task 'instanceBackup').
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

        const val TEMPORARY_DIR = "instance"
    }
}