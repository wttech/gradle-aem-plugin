package com.cognifide.gradle.aem.common.pkg

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.common.file.FileContentReader
import com.cognifide.gradle.common.utils.Patterns
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Input
import java.io.File
import java.io.Serializable

class PackageFileFilter(private val task: PackageCompose) : Serializable {

    private val aem = task.aem

    private val pkg get() = task.archiveFile.get().asFile

    @Input
    val excluding = aem.obj.boolean { convention(true) }

    /**
     * Exclude files being a part of CRX package.
     */
    @Input
    val excludeFiles = aem.obj.strings { convention(EXCLUDE_FILES_DEFAULT) }

    @Input
    val expanding = aem.obj.boolean { convention(true) }

    /**
     * Wildcard file name filter expression that is used to filter in which Vault files properties can be injected.
     */
    @Input
    val expandFiles = aem.obj.strings { convention(EXPAND_FILES_DEFAULT) }

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    val expandProperties = aem.obj.map<String, Any> { convention(mapOf()) }

    fun expandProperty(name: String, value: String) { expandProperties.put(name, value) }

    /**
     * Filter that ensures that only OSGi bundles will be put into CRX package under install path.
     */
    @Input
    val bundleChecking = aem.obj.typed<BundleChecking> {
        convention(BundleChecking.FAIL)
        aem.prop.string("package.fileFilter.bundleChecking")?.let { set(BundleChecking.of(it)) }
    }

    /**
     * Repository path pattern used in [bundleChecking].
     */
    @Input
    val bundlePath = aem.obj.string {
        convention("**/install/*.jar")
        aem.prop.string("package.fileFilter.bundlePath")?.let { set(it) }
    }

    fun filter(spec: CopySpec) {
        if (excluding.get()) {
            spec.exclude(excludeFiles.get())
        }

        spec.eachFile { fileDetail ->
            val path = "/${fileDetail.relativePath.pathString.removePrefix("/")}"

            if (expanding.get() && Patterns.wildcard(path, expandFiles.get())) {
                FileContentReader.filter(fileDetail) {
                    aem.prop.expand(it, expandPropertiesAll, path)
                }
            }

            if (bundleChecking.get() != BundleChecking.NONE && Patterns.wildcard(path, bundlePath.get())) {
                val bundle = fileDetail.file
                if (!isBundle(bundle)) {
                    val errorMessage = "JAR file being added to CRX package '$pkg' is not a valid OSGi bundle '$bundle'!"
                    when (bundleChecking.get()) {
                        BundleChecking.WARN -> {
                            aem.logger.warn(errorMessage)
                        }
                        BundleChecking.EXCLUDE -> {
                            aem.logger.info(errorMessage)
                            fileDetail.exclude()
                        }
                        BundleChecking.FAIL -> {
                            throw PackageException(errorMessage)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private val expandPropertiesAll by lazy { task.vaultDefinition.fileProperties + expandProperties.get() }

    @Suppress("TooGenericExceptionCaught")
    private fun isBundle(bundle: File): Boolean = try {
        val manifest = Jar(bundle).manifest.mainAttributes
        !manifest.getValue(Bundle.ATTRIBUTE_SYMBOLIC_NAME).isNullOrBlank()
    } catch (e: Exception) {
        false
    }

    companion object {
        val EXPAND_FILES_DEFAULT = listOf(
            "**/${Package.META_PATH}/MANIFEST.MF",
            "**/${Package.VLT_PATH}/nodetypes.cnd",
            "**/${Package.META_PATH}/**/*.xml"
        )

        val EXCLUDE_FILES_DEFAULT = listOf(
            "**/.gradle",
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.vlt",
            "**/.vlt*.tmp",
            "**/.vlt-sync-config.properties",
            "**/aemsync/**",
            "**/node_modules/**",
            "**/vault/filter.*.xml"
        )
    }
}
