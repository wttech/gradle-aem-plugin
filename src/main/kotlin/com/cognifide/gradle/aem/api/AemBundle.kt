package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.bundle.BundlePlugin
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

/**
 * DSL for easier manipulation of OSGi bundle JAR manifest attributes.
 */
class AemBundle(@Transient private val project: Project) {

    private val jar by lazy {
        (project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
                ?: throw AemException("Plugin '${BundlePlugin.ID}' is not applied.")) as Jar
    }

    fun attribute(name: String, value: String) {
        jar.manifest.attributes(mapOf(name to value))
    }

    fun exportPackage(pkg: String) {
        exportPackage(listOf(pkg))
    }

    fun exportPackage(pkgs: Collection<String>) {
        attribute("Export-Package", mergePackages(pkgs))
    }

    fun privatePackage(pkg: String) {
        privatePackage(listOf(pkg))
    }

    fun privatePackage(pkgs: Collection<String>) {
        attribute("Private-Package", mergePackages(pkgs))
    }

    private fun mergePackages(pkgs: Collection<String>): String {
        return pkgs.joinToString(",") { StringUtils.appendIfMissing(it, ".*") }
    }

}