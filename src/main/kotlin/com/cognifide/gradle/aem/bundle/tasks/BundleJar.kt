package com.cognifide.gradle.aem.bundle.tasks

import aQute.bnd.gradle.BundleTaskConvention as BndConvention
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.bundle.BundleException
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import com.cognifide.gradle.aem.common.utils.normalizeSeparators
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import java.io.File

@Suppress("LargeClass", "TooManyFunctions")
open class BundleJar(private val jar: Jar) {

    private val project = jar.project

    private val aem = project.aem

    @Nested
    val bndConvention = BndConvention(jar)

    /**
     * Allows to configure BND tool specific options.
     *
     * @see <https://bnd.bndtools.org>
     */
    fun bndTool(options: BndConvention.() -> Unit) {
        bndConvention.apply(options)
    }

    /**
     * Add instructions to the BND property from a list of multi-line strings.
     */
    @Suppress("SpreadOperator")
    fun bnd(vararg lines: CharSequence) = bndConvention.bnd(*lines)

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     */
    @Input
    val installPath = aem.obj.string { convention(aem.packageOptions.installPath) }

    /**
     * Suffix added to install path effectively allowing to install bundles only on specific instances.
     *
     * @see <https://helpx.adobe.com/experience-manager/6-4/sites/deploying/using/configure-runmodes.html#Definingadditionalbundlestobeinstalledforarunmode>
     */
    @Input
    @Optional
    val installRunMode = aem.obj.string()

    /**
     * Determines if Vault workspace filter entry pointing directly to JAR file should be added automatically
     * for built OSGi bundle.
     */
    @Input
    val vaultFilter = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("bundle.vaultFilter")?.let { set(it) }
    }

    /**
     * Enable or disable support for auto-generating OSGi specific JAR manifest attributes
     * like 'Bundle-SymbolicName', 'Export-Package' or AEM specific like 'Sling-Model-Packages'
     * using 'javaPackage' property.
     */
    @Input
    val attributesConvention = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("bundle.attributesConvention")?.let { set(it) }
    }

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
    val javaPackage = aem.obj.string {
        convention(aem.obj.provider {
            val group = aem.project.group.toString()
            val name = aem.project.name

            when {
                group.isNotBlank() -> "$group.$name"
                else -> name
            }.normalizeSeparators(".")
        })
    }

    /**
     * Determines how conflicts will be resolved when coincidental classes will be detected.
     * Useful to combine Java sources with Kotlin, Scala etc.
     *
     * @see <http://bnd.bndtools.org/heads/private_package.html>
     */
    @Input
    val javaPackageOptions = aem.obj.string {
        convention("-split-package:=merge-first")
        aem.prop.string("bundle.javaPackageOptions")?.let { set(it) }
    }

    @Internal
    val importPackages = aem.obj.strings { convention(listOf()) }

    @Input
    val importPackageWildcard = aem.obj.boolean { convention(true) }

    @Internal
    val exportPackages = aem.obj.strings { convention(listOf()) }

    @Internal
    val privatePackages = aem.obj.strings { convention(listOf()) }

    fun applyDefaults() {
        applyArchiveDefaults()
        applyBndToolDefaults()
    }

    private fun applyArchiveDefaults() {
        jar.archiveBaseName.set(aem.commonOptions.baseName)
    }

    private fun applyBndToolDefaults() {
        val instructionFile = File("${aem.project.file("bnd.bnd")}")
        if (instructionFile.isFile) {
            bndConvention.setBndfile(instructionFile)
        }

        bndConvention.bnd(mapOf(
                "-fixupmessages.bundleActivator" to "${Bundle.ATTRIBUTE_ACTIVATOR} * is being imported *;is:=error"
        ))
    }

    fun applyEvaluated() {
        applyAttributesConvention()
        combinePackageAttributes()
    }

    /**
     * Customize JAR being built to be valid OSGi bundle.
     */
    @Suppress("TooGenericExceptionCaught")
    @TaskAction
    fun runBndTool() {
        try {
            bndConvention.buildBundle()
        } catch (e: Exception) {
            aem.logger.error("BND tool error: https://bnd.bndtools.org", ExceptionUtils.getRootCause(e))
            throw BundleException("OSGi bundle cannot be built properly.", e)
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
        if (!attributesConvention.get()) {
            return
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_NAME) && !aem.project.description.isNullOrBlank()) {
            displayName = aem.project.description
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME) && !javaPackage.orNull.isNullOrBlank()) {
            symbolicName = javaPackage.get()
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES) && !javaPackage.orNull.isNullOrBlank()) {
            slingModelPackages = javaPackage.get()
        }

        if (!hasAttribute(Bundle.ATTRIBUTE_ACTIVATOR) && !javaPackage.orNull.isNullOrBlank()) {
            findActivator(javaPackage.get())?.let { activator = it }
        }
    }

    /**
     * Combine package attributes set explicitly with generated ones.
     */
    private fun combinePackageAttributes() {
        combinePackageAttribute(Bundle.ATTRIBUTE_IMPORT_PACKAGE, importPackages.get().toMutableList().apply {
            if (importPackageWildcard.get()) {
                add("*")
            }
        })
        combinePackageAttribute(Bundle.ATTRIBUTE_PRIVATE_PACKAGE, privatePackages.get())
        combinePackageAttribute(Bundle.ATTRIBUTE_EXPORT_PACKAGE, exportPackages.get().toMutableList().apply {
            if (attributesConvention.get() && !javaPackage.orNull.isNullOrBlank()) {
                add(when {
                    javaPackageOptions.isPresent -> "${javaPackage.get()}.*;${javaPackageOptions.get()}"
                    else -> "${javaPackage.get()}.*"
                })
            }
        })
    }

    @Input
    val activatorClasses = aem.obj.strings {
        convention(listOf("Activator", "BundleActivator"))
    }

    @Input
    val activatorSourceSets = aem.obj.map<String, String> {
        convention(mapOf(
                "java" to "java",
                "kotlin" to "kt",
                "scala" to "scala"
        ))
    }

    /**
     * Find activator class by conventional class names in source sets.
     */
    private fun findActivator(pkg: String): String? {
        for ((sourceSet, ext) in activatorSourceSets.get()) {
            for (activatorClass in activatorClasses.get()) {
                if (aem.project.file("src/main/$sourceSet/${pkg.replace(".", "/")}/$activatorClass.$ext").exists()) {
                    return "$pkg.$activatorClass"
                }
            }
        }

        return null
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
    var displayName: String?
        get() = attribute(Bundle.ATTRIBUTE_NAME)
        set(value) {
            attribute(Bundle.ATTRIBUTE_NAME, value)
        }

    @get:Internal
    var symbolicName: String?
        get() = attribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME)
        set(value) {
            attribute(Bundle.ATTRIBUTE_SYMBOLIC_NAME, value)
        }

    @get:Internal
    var activator: String?
        get() = attribute(Bundle.ATTRIBUTE_ACTIVATOR)
        set(value) {
            attribute(Bundle.ATTRIBUTE_ACTIVATOR, value)
        }

    @get:Internal
    var category: String?
        get() = attribute(Bundle.ATTRIBUTE_CATEGORY)
        set(value) {
            attribute(Bundle.ATTRIBUTE_CATEGORY, value)
        }

    @get:Internal
    var vendor: String?
        get() = attribute(Bundle.ATTRIBUTE_VENDOR)
        set(value) {
            attribute(Bundle.ATTRIBUTE_VENDOR, value)
        }

    @get:Internal
    var license: String?
        get() = attribute(Bundle.ATTRIBUTE_LICENSE)
        set(value) {
            attribute(Bundle.ATTRIBUTE_LICENSE, value)
        }

    @get:Internal
    var copyright: String?
        get() = attribute(Bundle.ATTRIBUTE_COPYRIGHT)
        set(value) {
            attribute(Bundle.ATTRIBUTE_COPYRIGHT, value)
        }

    @get:Internal
    var docUrl: String?
        get() = attribute(Bundle.ATTRIBUTE_DOC_URL)
        set(value) {
            attribute(Bundle.ATTRIBUTE_DOC_URL, value)
        }

    @get:Internal
    var developers: String?
        get() = attribute(Bundle.ATTRIBUTE_DEVELOPERS)
        set(value) {
            attribute(Bundle.ATTRIBUTE_DEVELOPERS, value)
        }

    @get:Internal
    var contributors: String?
        get() = attribute(Bundle.ATTRIBUTE_CONTRIBUTORS)
        set(value) {
            attribute(Bundle.ATTRIBUTE_CONTRIBUTORS, value)
        }

    @get:Internal
    var fragmentHost: String?
        get() = attribute(Bundle.ATTRIBUTE_FRAGMENT_HOST)
        set(value) {
            attribute(Bundle.ATTRIBUTE_FRAGMENT_HOST, value)
        }

    @get:Internal
    var slingModelPackages: String?
        get() = attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES)
        set(value) {
            attribute(Bundle.ATTRIBUTE_SLING_MODEL_PACKAGES, value)
        }

    fun exportPackage(pkgs: Iterable<String>) {
        exportPackages.addAll(pkgs)
    }

    fun exportPackage(vararg pkgs: String) = exportPackage(pkgs.toList())

    fun privatePackage(pkgs: Iterable<String>) {
        privatePackages.addAll(pkgs)
    }

    fun privatePackage(vararg pkgs: String) = privatePackage(pkgs.toList())

    fun excludePackage(pkgs: Iterable<String>) {
        importPackages.addAll(pkgs.map { "!$it" })
    }

    fun excludePackage(vararg pkgs: String) = excludePackage(pkgs.toList())

    fun importPackage(pkgs: Iterable<String>) {
        importPackages.addAll(pkgs)
    }

    fun importPackage(vararg pkgs: String) = importPackage(pkgs.toList())

    fun embedPackage(dependencyNotation: Any, vararg pkgs: String, export: Boolean = false) = embedPackage(dependencyNotation, pkgs.asIterable(), export)

    /**
     * Copy packages from external dependency (JAR) to currently built OSGi bundle.
     *
     * This utility method does not work when Gradle feature "configuration on demand" is enabled as of dependencies
     * should be configured before task is configured nor executed.
     *
     * @see <https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:configuration_on_demand>
     */
    fun embedPackage(dependencyNotation: Any, pkgs: Iterable<String>, export: Boolean = false) {
        project.dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, dependencyNotation)

        when {
            export -> exportPackage(pkgs)
            else -> privatePackage(pkgs)
        }
    }

    fun wildcardPackage(pkgs: Iterable<String>): List<String> = pkgs.map { StringUtils.appendIfMissing(it, ".*") }

    fun wildcardPackage(vararg pkgs: String): List<String> = wildcardPackage(pkgs.toList())
}

val Jar.bundle: BundleJar get() = convention.getPlugin(BundleJar::class.java)

fun Jar.bundle(action: BundleJar.() -> Unit) = this.bundle.apply(action)
