package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.osgi.OsgiPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File

open class UpdateManifestTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemUpdateManifest"
    }

    override val config = AemConfig.extendFromGlobal(project)

    private val manifest = project.convention.getPlugin(OsgiPluginConvention::class.java).osgiManifest()

    @TaskAction
    fun updateManifest() {
        includeEmbedJars()
        includeServiceComponents()
    }

    private fun addInstruction(name: String, valueProvider: () -> String) {
        if (!manifest.instructions.containsKey(name)) {
            val value = valueProvider()
            if (!value.isNullOrBlank()) {
                manifest.instruction(name, value)
            }
        }
    }

    // TODO test it!
    private fun includeEmbedJars() {
        val config = project.configurations.getByName(AemPlugin.CONFIG_EMBED)

        addInstruction("Bundle-ClassPath", { bundleClassPath(config) })
        addInstruction("Include-Resource", { includeResource(config) })
    }

    private fun bundleClassPath(configuration: Configuration): String {
        val list = mutableListOf(".")
        configuration.forEach { file -> list.add("${AemPlugin.OSGI_INF}/lib/${file.name}") }

        return list.joinToString { "," }
    }

    private fun includeResource(configuration: Configuration): String {
        return configuration.map { file -> "OSGI-INF/lib/${file.name}=${file.path}" }.joinToString { "," }
    }

    private fun includeServiceComponents() {
        addInstruction("Service-Component", { serviceComponentInstruction() })
    }

    private fun serviceComponentInstruction(): String {
        val jar = project.convention.getPlugin(JavaPluginConvention::class.java)
        val mainSourceSet = jar.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val osgiInfDir = File(mainSourceSet.output.classesDir, AemPlugin.OSGI_INF)
        val xmlFiles = osgiInfDir.listFiles({ _, name -> name.endsWith(".xml") })

        return xmlFiles.map { file -> "${AemPlugin.OSGI_INF}/${file.name}" }.joinToString(",")
    }

}