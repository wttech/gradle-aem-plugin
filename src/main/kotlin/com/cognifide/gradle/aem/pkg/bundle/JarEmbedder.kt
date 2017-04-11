package com.cognifide.gradle.aem.pkg.bundle

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.osgi.OsgiPlugin
import org.gradle.api.plugins.osgi.OsgiPluginConvention

class JarEmbedder(val project: Project) {

    fun embed() {
        val config = project.configurations.getByName(AemPlugin.CONFIG_EMBED)

        project.plugins.withType(OsgiPlugin::class.java) {
            fillBundleInstruction(project, "Bundle-ClassPath", { bundleClassPath(config) })
            fillBundleInstruction(project, "Include-Resource", { includeResource(config) })
        }
    }

    private fun fillBundleInstruction(project: Project, name: String, valueProvider: () -> String) {
        val manifest = project.convention.getPlugin(OsgiPluginConvention::class.java).osgiManifest()
        if (!manifest.instructions.containsKey(name)) {
            val value = valueProvider()
            manifest.instruction(name, value)
        }
    }

    /**
     * Collects class path dependencies to be used as bundle instruction 'Bundle-ClassPath'
     */
    private fun bundleClassPath(configuration: Configuration): String {
        val list = mutableListOf(".")
        configuration.forEach { file -> list.add("OSGI-INF/lib/${file.name}") }

        return list.joinToString { "," }
    }

    /**
     * Collects resources to be included in bundle instruction 'Include-Resource'
     */
    private fun includeResource(configuration: Configuration): String {
        return configuration.map { file -> "OSGI-INF/lib/${file.name}=${file.path}" }.joinToString { "," }
    }

}
