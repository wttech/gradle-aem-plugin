package com.cognifide.gradle.aem.pkg

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.FileContentReader
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Input
import java.io.Serializable

class ComposeFileFilter(project: Project) : Serializable {

    private val aem = AemExtension.of(project)

    @Input
    var excluding: Boolean = true

    /**
     * Exclude files being a part of CRX package.
     */
    @Input
    var excludeFiles: MutableList<String> = mutableListOf(
            "**/.gradle",
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.vlt",
            "**/.vlt*.tmp",
            "**/node_modules/**",
            "jcr_root/.vlt-sync-config.properties"
    )

    fun excludeFiles(files: List<String>) = excludeFiles.addAll(files)

    fun excludeFile(file: String) = excludeFiles.add(file)

    @Input
    var expanding: Boolean = true

    /**
     * Wildcard file name filter expression that is used to filter in which Vault files properties can be injected.
     */
    @Input
    var expandFiles: MutableList<String> = mutableListOf(
            "**/${PackagePlugin.VLT_PATH}/*.xml",
            "**/${PackagePlugin.VLT_PATH}/nodetypes.cnd"
    )

    fun expandFiles(files: List<String>) = expandFiles.addAll(files)

    fun expandFile(file: String) = expandFiles.add(file)

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    var expandProperties: MutableMap<String, Any> = mutableMapOf()

    fun expandProperties(properties: Map<String, Any>) = expandProperties.putAll(properties)

    fun expandProperty(name: String, value: String) = expandProperties(mapOf(name to value))

    /**
     * Filter that ensures that only OSGi bundles will be put into CRX package under install path.
     */
    @Input
    var bundleChecking: Boolean = true

    fun filter(spec: CopySpec, expandProperties: Map<String, Any> = mapOf()) {
        if (excluding) {
            spec.exclude(excludeFiles)
        }

        spec.eachFile { fileDetail ->
            val path = "/${fileDetail.relativePath.pathString.removePrefix("/")}"

            if (expanding) {
                if (Patterns.wildcard(path, expandFiles)) {
                    FileContentReader.filter(fileDetail) { aem.props.expandPackage(it, expandProperties + this.expandProperties, path) }
                }
            }

            if (bundleChecking) {
                if (Patterns.wildcard(path, "**/install/*.jar")) {
                    val bundle = fileDetail.file
                    val isBundle = try {
                        val manifest = Jar(bundle).manifest.mainAttributes
                        !manifest.getValue("Bundle-SymbolicName").isNullOrBlank()
                    } catch (e: Exception) {
                        false
                    }

                    if (!isBundle) {
                        aem.logger.warn("Jar being a part of composed CRX package is not a valid OSGi bundle: $bundle")
                        fileDetail.exclude()
                    }
                }
            }
        }
    }

}