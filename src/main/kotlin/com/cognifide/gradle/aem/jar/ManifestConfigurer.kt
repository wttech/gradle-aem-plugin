package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import org.dm.gradle.plugins.bundle.BundleExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Update manifest being used by 'jar' task of Java Plugin.
 *
 * Both plugins 'osgi' and 'org.dm.bundle' are supported.
 * Dependency embedding does not work when official 'osgi' plugin is used.
 *
 * @see <https://issues.gradle.org/browse/GRADLE-1107>
 * @see <https://github.com/TomDmitriev/gradle-bundle-plugin>
 */
class ManifestConfigurer(val project: Project) {

    companion object {
        val SERVICE_COMPONENT_INSTRUCTION = "Service-Component"

        val BUNDLE_CLASSPATH_INSTRUCTION = "Bundle-ClassPath"

        val INCLUDE_RESOURCE_INSTRUCTION = "Include-Resource"

        val OSGI_PLUGIN_ID = "osgi"

        val BUNDLE_PLUGIN_ID = "org.dm.bundle"
    }

    val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    val jarConvention = project.convention.getPlugin(JavaPluginConvention::class.java)!!

    val config = AemConfig.extendFromGlobal(project)

    fun configure() {
        includeEmbedJars()
        includeServiceComponents()
    }

    private fun osgiPluginApplied() = project.plugins.hasPlugin(OSGI_PLUGIN_ID)

    private fun bundlePluginApplied() = project.plugins.hasPlugin(BUNDLE_PLUGIN_ID)

    private fun addInstruction(name: String, valueProvider: () -> String) {
        if (osgiPluginApplied()) {
            addInstruction(jar.manifest as OsgiManifest, name, valueProvider())
        } else if (bundlePluginApplied()) {
            addInstruction(project.extensions.getByType(BundleExtension::class.java), name, valueProvider())
        } else {
            project.logger.warn("Cannot apply specific OSGi instruction to JAR manifest, because neither "
                    + "'$OSGI_PLUGIN_ID' nor '$BUNDLE_PLUGIN_ID' are applied to project '${project.name}'.")
        }
    }

    private fun addInstruction(manifest: OsgiManifest, name: String, value: String) {
        if (!manifest.instructions.containsKey(name)) {
            if (!value.isNullOrBlank()) {
                manifest.instruction(name, value)
            }
        }
    }

    @Suppress("unchecked_cast")
    private fun addInstruction(config: BundleExtension, name: String, value: String) {
        val instructions = config.instructions as Map<String, Any>
        if (!instructions.contains(name)) {
            if (!value.isNullOrBlank()) {
                config.instruction(name, value)
            }
        }
    }

    private fun includeEmbedJars() {
        if (osgiPluginApplied()) {
            project.logger.warn("As of Gradle 3.5, jar embedding does not work when 'osgi' plugin is used."
                    + " Consider using 'org.dm.bundle' instead.")
        }

        val config = jar.project.configurations.getByName(AemPlugin.CONFIG_EMBED)
        val files = config.files

        addInstruction(BUNDLE_CLASSPATH_INSTRUCTION, { bundleClassPath(files) })
        addInstruction(INCLUDE_RESOURCE_INSTRUCTION, { includeResource(files) })
    }

    private fun bundleClassPath(files: Set<File>): String {
        val list = mutableListOf(".")
        files.onEach { file -> list.add("${AemPlugin.OSGI_EMBED}/${file.name}") }

        return list.joinToString(",")
    }

    private fun includeResource(files: Set<File>): String {
        return files.map { file -> "${AemPlugin.OSGI_EMBED}/${file.name}" }.joinToString(",")
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