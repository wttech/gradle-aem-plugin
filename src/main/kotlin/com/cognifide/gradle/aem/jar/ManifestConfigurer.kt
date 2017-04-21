package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Update manifest being used by 'jar' task of Java Plugin.
 */
class ManifestConfigurer(val project: Project) {

    companion object {
        val SERVICE_COMPONENT_INSTRUCTION = "Service-Component"

        val BUNDLE_CLASSPATH_INSTRUCTION = "Bundle-ClassPath"

        val INCLUDE_RESOURCE_INSTRUCTION = "Include-Resource"
    }

    val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    val jarConvention = project.convention.getPlugin(JavaPluginConvention::class.java)!!

    val osgiManifest = jar.manifest as OsgiManifest

    val config = AemConfig.extendFromGlobal(project)

    fun configure() {
        includeEmbedJars()
        includeServiceComponents()
    }

    private fun addInstruction(name: String, valueProvider: () -> String) {
        if (!osgiManifest.instructions.containsKey(name)) {
            val value = valueProvider()
            if (!value.isNullOrBlank()) {
                osgiManifest.instruction(name, value)
            }
        }
    }

    private fun includeEmbedJars() {
        val config = jar.project.configurations.getByName(AemPlugin.CONFIG_EMBED)

        addInstruction(BUNDLE_CLASSPATH_INSTRUCTION, { bundleClassPath(config) })
        addInstruction(INCLUDE_RESOURCE_INSTRUCTION, { includeResource(config) })
    }

    private fun bundleClassPath(configuration: Configuration): String {
        val list = mutableListOf(".")
        configuration.forEach { file -> list.add("${AemPlugin.OSGI_EMBED}/${file.name}") }

        return list.joinToString { "," }
    }

    private fun includeResource(configuration: Configuration): String {
        return configuration.map { file -> "${AemPlugin.OSGI_EMBED}/${file.name}=${file.path}" }.joinToString { "," }
    }

    private fun includeServiceComponents() {
        addInstruction(SERVICE_COMPONENT_INSTRUCTION, { serviceComponentInstruction() })
    }

    private fun serviceComponentInstruction(): String {
        val mainSourceSet = jarConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val osgiInfDir = File(mainSourceSet.output.classesDir, AemPlugin.OSGI_INF)
        val xmlFiles = osgiInfDir.listFiles({ _, name -> name.endsWith(".xml") })

        return xmlFiles.map { file -> "${AemPlugin.OSGI_INF}/${file.name}" }.joinToString(",")
    }

}