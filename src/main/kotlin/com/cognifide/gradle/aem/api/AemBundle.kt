package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.internal.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Jar

/**
 * DSL for easier manipulation of OSGi bundle JAR manifest attributes.
 *
 * The main purpose of this extension point is to provide a place for specifying custom
 * OSGi bundle related properties, because it is not possible to add properties to existing tasks
 * like 'jar' directly.
 */
class AemBundle(
        @Transient
        @JsonIgnore
        private val project: Project
) {

    private val jar by lazy {
        (project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
                ?: throw AemException("Plugin '${BundlePlugin.ID}' is not applied.")) as Jar
    }

    /**
     * Determines package in which OSGi bundle being built contains its classes.
     * Basing on that value, there will be:
     *
     * - generated OSGi specific manifest instructions like 'Bundle-SymbolicName', 'Export-Package'.
     * - generated AEM specific manifest instructions like 'Sling-Model-Packages'.
     * - performed additional component stability checks during 'aemAwait'
     *
     * Default convention: '${project.group}.${project.name}'
     */
    @Input
    var javaPackage: String = AemExtension.AUTO_DETERMINED

    @get:Internal
    @get:JsonIgnore
    val javaPackageDefault: String
        get() {
            if ("${project.group}".isBlank()) {
                throw AemException("${project.displayName.capitalize()} must has property 'group' defined to determine bundle package default.")
            }

            return Formats.normalizeSeparators("${project.group}.${project.name}", ".")
        }

    /**
     * Determines how conflicts will be resolved when coincidental classes will be detected.
     * Useful to combine Java sources with Kotlin, Scala etc.
     *
     * @see <http://bnd.bndtools.org/heads/private_package.html>
     */
    @Input
    var javaPackageOptions: String = "-split-package:=merge-first"

    /**
     * Enable or disable support for auto-generating OSGi specific JAR manifest attributes
     * like 'Bundle-SymbolicName', 'Export-Package' or AEM specific like 'Sling-Model-Packages'
     * using 'bundlePackage' property.
     */
    @Input
    var manifestAttributes: Boolean = true

    /**
     * Bundle instructions file location consumed by BND tool.
     *
     * If file exists, instructions will be taken from it instead of directly specified
     * in dedicated property.
     *
     * @see <https://bnd.bndtools.org>
     */
    @Input
    var bndPath: String = "${project.file("bnd.bnd")}"

    /**
     * Bundle instructions consumed by BND tool (still file has precedence).
     *
     * By default, plugin is increasing an importance of some warning so that it will
     * fail a build instead just logging it.
     *
     * @see <https://bnd.bndtools.org/chapters/825-instructions-ref.html>
     */
    @Input
    var bndInstructions: MutableMap<String, Any> = mutableMapOf(
            "-fixupmessages.bundleActivator" to "$ATTRIBUTE_ACTIVATOR * is being imported *;is:=error"
    )

    init {
        project.afterEvaluate {
            if (javaPackage == AemExtension.AUTO_DETERMINED) {
                javaPackage = javaPackageDefault
            }
        }
    }

    fun attribute(name: String, value: String) = jar.manifest.attributes(mapOf(name to value))

    fun name(name: String) = attribute(ATTRIBUTE_NAME, name)

    fun symbolicName(name: String) = attribute(ATTRIBUTE_SYMBOLIC_NAME, name)

    fun manifestVersion(num: Int) = attribute(ATTRIBUTE_MANIFEST_VERSION, num.toString())

    fun activator(className: String) = attribute(ATTRIBUTE_ACTIVATOR, className)

    fun category(name: String) = attribute(ATTRIBUTE_CATEGORY, name)

    fun vendor(name: String) = attribute(ATTRIBUTE_VENDOR, name)

    fun exportPackage(pkg: String) = exportPackage(listOf(pkg))

    fun exportPackage(vararg pkgs: String) = exportPackage(pkgs.toList())

    fun exportPackage(pkgs: Collection<String>) {
        attribute(ATTRIBUTE_EXPORT_PACKAGE, wildcardPackages(pkgs))
    }

    fun privatePackage(pkg: String) = privatePackage(listOf(pkg))

    fun privatePackage(vararg pkgs: String) = privatePackage(pkgs.toList())

    fun privatePackage(pkgs: Collection<String>) {
        attribute(ATTRIBUTE_PRIVATE_PACKAGE, wildcardPackages(pkgs))
    }

    fun excludePackage(vararg pkgs: String) {
        excludePackage(pkgs.toList())
    }

    fun excludePackage(pkgs: Collection<String>) {
        attribute(ATTRIBUTE_IMPORT_PACKAGE, mergePackages(pkgs.map { "!$it" } + "*"))
    }

    fun wildcardPackages(pkgs: Collection<String>): String {
        return pkgs.joinToString(",") { StringUtils.appendIfMissing(it, ".*") }
    }

    fun mergePackages(pkgs: Collection<String>): String {
        return pkgs.joinToString(",")
    }

    companion object {

        const val ATTRIBUTE_NAME = "Bundle-Name"

        const val ATTRIBUTE_SYMBOLIC_NAME = "Bundle-SymbolicName"

        const val ATTRIBUTE_MANIFEST_VERSION = "Bundle-ManifestVersion"

        const val ATTRIBUTE_ACTIVATOR = "Bundle-Activator"

        const val ATTRIBUTE_CATEGORY = "Bundle-Category"

        const val ATTRIBUTE_VENDOR = "Bundle-Vendor"

        const val ATTRIBUTE_EXPORT_PACKAGE = "Export-Package"

        const val ATTRIBUTE_PRIVATE_PACKAGE = "Private-Package"

        const val ATTRIBUTE_IMPORT_PACKAGE = "Import-Package"

        const val ATTRIBUTE_SLING_MODEL_PACKAGES = "Sling-Model-Packages"

     }

}