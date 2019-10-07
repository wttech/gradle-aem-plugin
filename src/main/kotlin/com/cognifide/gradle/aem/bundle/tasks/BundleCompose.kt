package com.cognifide.gradle.aem.bundle.tasks

import aQute.bnd.gradle.BundleTaskConvention
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.bundle.BundleException
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import aQute.bnd.gradle.Bundle as Base
import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.lang.MetaClass
import java.io.File
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*

@Suppress("LargeClass", "TooManyFunctions")
class BundleCompose : Base(), AemTask {

    @Internal
    final override val aem = AemExtension.of(project)

    @Internal
    val bundleConvention = convention.plugins["bundle"] as BundleTaskConvention

    @Internal
    val javaConvention = convention.plugins["java"] as JavaPluginConvention

    /**
     * Allows to configure BND tool specific options.
     *
     * @see <https://bnd.bndtools.org>
     */
    fun bndTool(options: BundleTaskConvention.() -> Unit) {
        bundleConvention.apply(options)
    }

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     */
    @Input
    var installPath: String = aem.packageOptions.installPath

    /**
     * Suffix added to install path effectively allowing to install bundles only on specific instances.
     *
     * @see <https://helpx.adobe.com/experience-manager/6-4/sites/deploying/using/configure-runmodes.html#Definingadditionalbundlestobeinstalledforarunmode>
     */
    @Input
    @Optional
    var installRunMode: String? = null

    /**
     * Determines if Vault workspace filter entry pointing directly to JAR file should be added automatically
     * for built OSGi bundle.
     */
    @Input
    @Optional
    var vaultFilter: Boolean = true

    /**
     * Enable or disable support for auto-generating OSGi specific JAR manifest attributes
     * like 'Bundle-SymbolicName', 'Export-Package' or AEM specific like 'Sling-Model-Packages'
     * using 'javaPackage' property.
     */
    @Input
    var attributesConvention: Boolean = true

    /**
     * Determines package in which OSGi bundle being built contains its classes.
     * Basing on that value, there will be:
     *
     * - generated OSGi specific manifest instructions like 'Bundle-SymbolicName', 'Export-Package'.
     * - generated AEM specific manifest instructions like 'Sling-Model-Packages'.
     * - performed additional component stability checks within 'packageDeploy' or separately using 'instanceAwait'.
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

    @Internal
    @JsonIgnore
    var importPackages: List<String> = listOf("*")

    @Internal
    @JsonIgnore
    var exportPackages: List<String> = listOf()

    @Internal
    @JsonIgnore
    var privatePackages: List<String> = listOf()

    init {
        applyArchiveDefaults()
        applyBndToolDefaults()
    }

    private fun applyArchiveDefaults() {
        destinationDirectory.set(AemTask.temporaryDir(aem.project, name))
        archiveBaseName.set(aem.baseName)
        from(javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output)
    }

    private fun applyBndToolDefaults() {
        val instructionFile = File("${aem.project.file("bnd.bnd")}")
        if (instructionFile.isFile) {
            bundleConvention.setBndfile(instructionFile)
        }

        bundleConvention.bnd(mapOf(
                "-fixupmessages.bundleActivator" to "${Bundle.ATTRIBUTE_ACTIVATOR} * is being imported *;is:=error"
        ))
    }

    override fun projectEvaluated() {
        super.projectEvaluated()

        ensureJavaPackage()
        applyAttributesConvention()
        combinePackageAttributes()
    }

    private fun ensureJavaPackage() {
        if (javaPackage == null) {
            if ("${aem.project.group}".isBlank()) {
                throw AemException("${aem.project.displayName.capitalize()} must has property 'group' defined" +
                        " to determine bundle package default.")
            }

            javaPackage = Formats.normalizeSeparators("${aem.project.group}.${aem.project.name}", ".")
        }
    }

    private fun combinePackageAttribute(name: String, pkgs: Iterable<String>) {
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
    private fun applyAttributesConvention() {
        if (!attributesConvention) {
            return
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_NAME) && !aem.project.description.isNullOrBlank()) {
            displayName = aem.project.description
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME) && !javaPackage.isNullOrBlank()) {
            symbolicName = javaPackage
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES) && !javaPackage.isNullOrBlank()) {
            slingModelPackages = javaPackage
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_ACTIVATOR) && !javaPackage.isNullOrBlank()) {
            findActivator(javaPackage!!)?.let { activator = it }
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

    private fun findActivator(pkg: String): String? {
        for ((sourceSet, ext) in SOURCE_SETS) {
            for (activatorClass in ACTIVATOR_CLASSES) {
                if (aem.project.file("src/main/$sourceSet/${pkg.replace(".", "/")}/$activatorClass.$ext").exists()) {
                    return "$pkg.$activatorClass"
                }
            }
        }

        return null
    }

    @get:Input
    var attributes: MutableMap<String, Any?>
        get() = mutableMapOf<String, Any?>().apply { putAll(manifest.attributes) }
        set(value) {
            manifest.attributes(value)
        }

    fun attribute(name: String, value: String?) = manifest.attributes(mapOf(name to value))

    fun attribute(name: String): String? = manifest.attributes[name] as String?

    fun hasAttribute(name: String) = attributes.containsKey(name)

    @get:Internal
    @get:JsonIgnore
    var displayName: String?
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
        get() = attribute(Bundle.ATTRIBUTE_ACTIVATOR)
        set(value) {
            attribute(Bundle.ATTRIBUTE_ACTIVATOR, value)
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
    var slingModelPackages: String?
        get() = attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES)
        set(value) {
            attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES, value)
        }

    fun exportPackage(pkg: String) = exportPackages(pkg)

    fun exportPackages(pkgs: Iterable<String>) {
        exportPackages += pkgs
    }

    fun exportPackages(vararg pkgs: String) = exportPackages(pkgs.toList())

    fun privatePackage(pkg: String) = privatePackages(listOf(pkg))

    fun privatePackages(pkgs: Iterable<String>) {
        privatePackages += pkgs
    }

    fun privatePackages(vararg pkgs: String) = privatePackages(pkgs.toList())

    fun excludePackage(pkg: String) = excludePackages(listOf(pkg))

    fun excludePackages(pkgs: Iterable<String>) {
        importPackages += pkgs.map { "!$it" }
    }

    fun excludePackages(vararg pkgs: String) = excludePackages(pkgs.toList())

    fun importPackage(pkg: String) = importPackages(listOf(pkg))

    fun importPackages(pkgs: Iterable<String>) {
        importPackages += pkgs
    }

    fun importPackages(vararg pkgs: String) = importPackages(pkgs.toList())

    fun embedPackage(pkg: String, export: Boolean, dependencyOptions: DependencyOptions.() -> Unit) {
        embedPackages(listOf(pkg), export, dependencyOptions)
    }

    fun embedPackages(vararg pkgs: String, export: Boolean, dependencyOptions: DependencyOptions.() -> Unit) {
        embedPackages(pkgs.asIterable(), export, dependencyOptions)
    }

    fun embedPackages(pkgs: Iterable<String>, export: Boolean, dependencyOptions: DependencyOptions.() -> Unit) {
        embedPackages(pkgs, export, DependencyOptions.create(aem, dependencyOptions))
    }

    fun embedPackage(pkg: String, export: Boolean, dependencyNotation: String) {
        embedPackages(listOf(pkg), export, dependencyNotation)
    }

    fun embedPackages(vararg pkgs: String, export: Boolean, dependencyNotation: String) {
        embedPackages(pkgs.asIterable(), export, dependencyNotation)
    }

    fun embedPackages(pkgs: Iterable<String>, export: Boolean, dependencyNotation: String) {
        embedPackages(pkgs, export, dependencyNotation as Any)
    }

    private fun embedPackages(pkgs: Iterable<String>, export: Boolean, dependencyNotation: Any) {
        aem.project.dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, dependencyNotation)

        if (export) {
            exportPackages(pkgs)
        } else {
            privatePackages(pkgs)
        }
    }

    fun wildcardPackages(pkgs: Iterable<String>): List<String> {
        return pkgs.map { StringUtils.appendIfMissing(it, ".*") }
    }

    fun wildcardPackages(vararg pkgs: String): List<String> {
        return wildcardPackages(pkgs.toList())
    }

    @TaskAction
    @Suppress("TooGenericExceptionCaught")
    override fun copy() {
        try {
            super.copy()
        } catch (e: Exception) {
            aem.logger.error("Bundle error: https://bnd.bndtools.org", ExceptionUtils.getRootCause(e))
            throw BundleException("OSGi bundle cannot be built properly.", e)
        }
    }

    // TODO https://github.com/bndtools/bnd/issues/3470

    override fun invokeMethod(p0: String?, p1: Any?): Any {
        TODO("not implemented")
    }

    override fun setMetaClass(p0: MetaClass?) {
        TODO("not implemented")
    }

    override fun getMetaClass(): MetaClass {
        TODO("not implemented")
    }

    override fun getProperty(p0: String?): Any {
        TODO("not implemented")
    }

    companion object {
        const val NAME = "bundleCompose"

        val ACTIVATOR_CLASSES = listOf("Activator", "BundleActivator")

        val SOURCE_SETS = mapOf(
                "java" to "java",
                "kotlin" to "kt",
                "scala" to "scala"
        )
    }
}
