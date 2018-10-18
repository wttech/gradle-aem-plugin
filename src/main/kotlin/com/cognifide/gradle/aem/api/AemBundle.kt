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
class AemBundle(@Transient private val project: Project) {

    private val jar by lazy {
        (project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
                ?: throw AemException("Plugin '${BundlePlugin.ID}' is not applied.")) as Jar
    }

    private val config = AemConfig.of(project)

    /**
     * Content path for bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default.
     */
    @Input
    var installPath: String = if (project == project.rootProject) {
        "/apps/${project.rootProject.name}/install"
    } else {
        "/apps/${project.rootProject.name}/${config.projectName}/install"
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
    var javaPackage: String = AemConfig.AUTO_DETERMINED

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
            "-fixupmessages.bundleActivator" to "Bundle-Activator * is being imported *;is:=error"
    )

    init {
        project.afterEvaluate {
            if (javaPackage == AemConfig.AUTO_DETERMINED) {
                javaPackage = javaPackageDefault
            }

            if (installPath.isBlank()) {
                throw AemException("Bundle path cannot be blank")
            }
        }
    }

    fun attribute(name: String, value: String) {
        jar.manifest.attributes(mapOf(name to value))
    }

    fun exportPackage(pkg: String) {
        exportPackage(listOf(pkg))
    }

    fun exportPackage(vararg pkgs: String) {
        exportPackage(pkgs.toList())
    }

    fun exportPackage(pkgs: Collection<String>) {
        attribute("Export-Package", wildcardPackages(pkgs))
    }

    fun privatePackage(pkg: String) {
        privatePackage(listOf(pkg))
    }

    fun privatePackage(vararg pkgs: String) {
        privatePackage(pkgs.toList())
    }

    fun privatePackage(pkgs: Collection<String>) {
        attribute("Private-Package", wildcardPackages(pkgs))
    }

    fun excludePackage(vararg pkgs: String) {
        excludePackage(pkgs.toList())
    }

    fun excludePackage(pkgs: Collection<String>) {
        attribute("Import-Package", mergePackages(pkgs.map { "!$it" } + "*"))
    }

    fun wildcardPackages(pkgs: Collection<String>): String {
        return pkgs.joinToString(",") { StringUtils.appendIfMissing(it, ".*") }
    }

    fun mergePackages(pkgs: Collection<String>): String {
        return pkgs.joinToString(",")
    }

    companion object {

        fun of(project: Project): AemBundle {
            return AemExtension.of(project).bundle
        }

    }

}