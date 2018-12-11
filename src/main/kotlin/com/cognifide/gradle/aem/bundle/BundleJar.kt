package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.common.*
import com.cognifide.gradle.aem.instance.Bundle
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Jar

/**
 * The main purpose of this extension point is to provide a place for specifying custom
 * OSGi bundle related properties, because it is not possible to add properties to existing tasks
 * like 'jar' directly.
 */
class BundleJar(
    @Transient
@JsonIgnore
private val aem: AemExtension,

    @Internal
@Transient
@JsonIgnore
val jar: Jar
) : Serializable {

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default.
     */
    @Input
    var installPath: String = aem.config.packageInstallPath

    /**
     * Enable or disable support for auto-generating OSGi specific JAR manifest attributes
     * like 'Bundle-SymbolicName', 'Export-Package' or AEM specific like 'Sling-Model-Packages'
     * using 'bundlePackage' property.
     */
    @Input
    var attributesConvention: Boolean = true

    /**
     * Determines package in which OSGi bundle being built contains its classes.
     * Basing on that value, there will be:
     *
     * - generated OSGi specific manifest instructions like 'Bundle-SymbolicName', 'Export-Package'.
     * - generated AEM specific manifest instructions like 'Sling-Model-Packages'.
     * - performed additional component stability checks during 'aemAwait'
     *
     * Default convention: '${project.group}.${project.name}'.
     *
     * Use empty string to disable automatic determining of that package and going further OSGi components checks.
     */
    @Input
    var javaPackage: String? = null

    /**
     * Determines how conflicts will be resolved when coincidental classes will be detected.
     * Useful to combine Java sources with Kotlin, Scala etc.
     *
     * @see <http://bnd.bndtools.org/heads/private_package.html>
     */
    @Input
    var javaPackageOptions: String = "-split-package:=merge-first"

    /**
     * Bundle instructions file location consumed by BND tool.
     *
     * If file exists, instructions will be taken from it instead of directly specified
     * in dedicated property.
     *
     * @see <https://bnd.bndtools.org>
     */
    @Input
    var bndPath: String = "${aem.project.file("bnd.bnd")}"

    /**
     * Bundle instructions consumed by BND tool (still file has precedence).
     *
     * By default, plugin is increasing an importance of some warning so that it will
     * fail a build instead just logging it.
     *
     * @see <https://bnd.bndtools.org/chapters/825-instructions-ref.html>
     */
    @Input
    var bndInstructions: Map<String, Any> = mapOf(
            "-fixupmessages.bundleActivator" to "${Bundle.ATTRIBUTE_ACTIVATOR} * is being imported *;is:=error"
    )

    @Internal
    @JsonIgnore
    var importPackages: List<String> = listOf("*")

    @Internal
    @JsonIgnore
    var exportPackages: List<String> = listOf()

    @Internal
    @JsonIgnore
    var privatePackages: List<String> = listOf()

    fun projectsEvaluated() {
        ensureJavaPackage()
        ensureBaseNameIfNotCustomized()
        applyConventionAttributes()
        combinePackageAttributes()
    }

    private fun ensureJavaPackage() {
        if (javaPackage == null) {
            if ("${aem.project.group}".isBlank()) {
                throw AemException("${aem.project.displayName.capitalize()} must has property 'group' defined to determine bundle package default.")
            }

            javaPackage = Formats.normalizeSeparators("${aem.project.group}.${aem.project.name}", ".")
        }
    }

    /**
     * Reflection is used, because in other way, default convention will provide value.
     * It is only way to know, if base name was previously customized by build script.
     */
    private fun ensureBaseNameIfNotCustomized() {
        val baseName = FieldUtils.readField(jar, "baseName", true) as String?
        if (baseName.isNullOrBlank()) {
            val groupValue = aem.project.group as String?
            if (!aem.project.name.isNullOrBlank() && !groupValue.isNullOrBlank()) {
                jar.baseName = aem.baseName
            }
        }
    }

    /**
     * Combine package attributes set explicitly with generated ones.
     */
    private fun combinePackageAttributes() {
        combinePackageAttribute(Bundle.ATTRIBUTE_IMPORT_PACKAGE, importPackages)
        combinePackageAttribute(Bundle.ATTRIBUTE_PRIVATE_PACKAGE, privatePackages)
        combinePackageAttribute(Bundle.ATTRIBUTE_EXPORT_PACKAGE, exportPackages.toMutableList().apply {
            if (attributesConvention && !javaPackage.isNullOrBlank()) {
                val javaPackageExported = if (javaPackageOptions.isNotBlank()) {
                    "$javaPackage.*;$javaPackageOptions"
                } else {
                    "$javaPackage.*"
                }

                add(javaPackageExported)
            }
        })
    }

    private fun combinePackageAttribute(name: String, pkgs: Collection<String>) {
        val combinedPkgs = mutableSetOf<String>().apply {
            val existing = (attribute(name) ?: "")
                    .split(",")
                    .map { it.trim() }

            addAll(pkgs)
            addAll(existing)
        }.filter { it.isNotBlank() }

        if (combinedPkgs.isNotEmpty()) {
            attribute(name, combinedPkgs.joinToString(","))
        }
    }

    /**
     * Generate attributes by convention using Gradle project metadata.
     */
    private fun applyConventionAttributes() {
        if (!attributesConvention) {
            return
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_NAME) && !aem.project.description.isNullOrBlank()) {
            name = aem.project.description
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME) && !javaPackage.isNullOrBlank()) {
            symbolicName = javaPackage
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES) && !javaPackage.isNullOrBlank()) {
            slingModelPackages = javaPackage
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_CREATED_BY)) {
            createdBy = AemPlugin.NAME
        }
    }

    @get:Input
    var attributes: MutableMap<String, Any?>
        get() = mutableMapOf<String, Any?>().apply { putAll(jar.manifest.attributes) }
        set(value) {
            jar.manifest.attributes(value)
        }

    fun attribute(name: String, value: String?) = jar.manifest.attributes(mapOf(name to value))

    fun attribute(name: String): String? = jar.manifest.attributes[name] as String?

    fun hasAttribute(name: String) = attributes.containsKey(name)

    @get:Internal
    @get:JsonIgnore
    var name: String?
        get() = attribute(Bundle.ATTRIBUTE_NAME)
        set(value) {
            attribute(Bundle.ATTRIBUTE_NAME, value)
        }

    @get:Internal
    @get:JsonIgnore
    var symbolicName: String?
        get() = attribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME)
        set(value) {
            attribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME, value)
        }

    @get:Internal
    @get:JsonIgnore
    var activator: String?
        get() = attributes[Bundle.ATTRIBUTE_ACTIVATOR]?.toString()
        set(value) {
            attributes[Bundle.ATTRIBUTE_ACTIVATOR] = value
        }

    @get:Internal
    @get:JsonIgnore
    var category: String?
        get() = attribute(Bundle.ATTRIBUTE_CATEGORY)
        set(value) {
            attribute(Bundle.ATTRIBUTE_CATEGORY, value)
        }

    @get:Internal
    @get:JsonIgnore
    var vendor: String?
        get() = attribute(Bundle.ATTRIBUTE_VENDOR)
        set(value) {
            attribute(Bundle.ATTRIBUTE_VENDOR, value)
        }

    @get:Internal
    @get:JsonIgnore
    var license: String?
        get() = attribute(Bundle.ATTRIBUTE_LICENSE)
        set(value) {
            attribute(Bundle.ATTRIBUTE_LICENSE, value)
        }

    @get:Internal
    @get:JsonIgnore
    var copyright: String?
        get() = attribute(Bundle.ATTRIBUTE_COPYRIGHT)
        set(value) {
            attribute(Bundle.ATTRIBUTE_COPYRIGHT, value)
        }

    @get:Internal
    @get:JsonIgnore
    var docUrl: String?
        get() = attribute(Bundle.ATTRIBUTE_DOC_URL)
        set(value) {
            attribute(Bundle.ATTRIBUTE_DOC_URL, value)
        }

    @get:Internal
    @get:JsonIgnore
    var developers: String?
        get() = attribute(Bundle.ATTRIBUTE_DEVELOPERS)
        set(value) {
            attribute(Bundle.ATTRIBUTE_DEVELOPERS, value)
        }

    @get:Internal
    @get:JsonIgnore
    var contributors: String?
        get() = attribute(Bundle.ATTRIBUTE_CONTRIBUTORS)
        set(value) {
            attribute(Bundle.ATTRIBUTE_CONTRIBUTORS, value)
        }

    @get:Internal
    @get:JsonIgnore
    var fragmentHost: String?
        get() = attribute(Bundle.ATTRIBUTE_FRAGMENT_HOST)
        set(value) {
            attribute(Bundle.ATTRIBUTE_FRAGMENT_HOST, value)
        }

    @get:Internal
    @get:JsonIgnore
    var createdBy: String?
        get() = attribute(Bundle.ATTRIBUTE_CREATED_BY)
        set(value) {
            attribute(Bundle.ATTRIBUTE_CREATED_BY, value)
        }

    @get:Internal
    @get:JsonIgnore
    var slingModelPackages: String?
        get() = attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES)
        set(value) {
            attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES, value)
        }

    fun exportPackage(pkg: String) = exportPackages(listOf(pkg))

    fun exportPackages(pkgs: Collection<String>) {
        exportPackages += pkgs
    }

    fun privatePackage(pkg: String) = privatePackages(listOf(pkg))

    fun privatePackages(pkgs: Collection<String>) {
        privatePackages += pkgs
    }

    fun excludePackage(pkg: String) = excludePackages(listOf(pkg))

    fun excludePackages(pkgs: Collection<String>) {
        importPackages += pkgs.map { "!$it" }
    }

    fun embedPackage(pkg: String, export: Boolean, dependencyOptions: DependencyOptions.() -> Unit) {
        embedPackage(pkg, export, DependencyOptions.of(aem.project.dependencies, dependencyOptions))
    }

    fun embedPackage(pkg: String, export: Boolean, dependencyNotation: Any) {
        embedPackages(listOf(pkg), export, dependencyNotation)
    }

    fun embedPackages(pkgs: Collection<String>, export: Boolean, dependencyNotation: Any) {
        aem.project.dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, dependencyNotation)

        if (export) {
            exportPackages(pkgs)
        } else {
            privatePackages(pkgs)
        }
    }

    fun wildcardPackages(pkgs: Collection<String>): List<String> {
        return pkgs.map { StringUtils.appendIfMissing(it, ".*") }
    }
}