package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.dm.gradle.plugins.bundle.BundleExtension
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
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
open class UpdateManifestTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemUpdateManifest"

        val SERVICE_COMPONENT_INSTRUCTION = "Service-Component"

        val BUNDLE_CLASSPATH_INSTRUCTION = "Bundle-ClassPath"

        val INCLUDE_RESOURCE_INSTRUCTION = "Include-Resource"

        val OSGI_PLUGIN_ID = "osgi"

        val BUNDLE_PLUGIN_ID = "org.dm.bundle"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Update OSGi manifest instructions"
    }

    override val config = AemConfig.extendFromGlobal(project)

    val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar

    val jarConvention = project.convention.getPlugin(JavaPluginConvention::class.java)!!

    val embeddableJars: List<File>
        @InputFiles
        get() {
            return jar.project.configurations.getByName(AemPlugin.CONFIG_EMBED).files.sortedBy { it.name }
        }

    val serviceComponents: List<File>
        @InputFiles
        get() {
            val mainSourceSet = jarConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            val osgiInfDir = File(mainSourceSet.output.classesDir, AemPlugin.OSGI_INF)

            return osgiInfDir.listFiles({ _, name -> name.endsWith(".xml") }).toList().sortedBy { it.name }
        }

    @TaskAction
    fun updateManifest() {
        includeEmbedJars()
        includeServiceComponents()
    }

    private fun includeEmbedJars() {
        if (embeddableJars.isEmpty()) {
            return
        }

        if (osgiPluginApplied()) {
            project.logger.warn("As of Gradle 3.5, jar embedding does not work when 'osgi' plugin is used."
                    + " Consider using 'org.dm.bundle' instead.")
        }

        project.logger.info("Embedding jar files: ${embeddableJars.map { it.name }}")

        addInstruction(BUNDLE_CLASSPATH_INSTRUCTION, { bundleClassPath() })
        addInstruction(INCLUDE_RESOURCE_INSTRUCTION, { includeResource() })
    }

    private fun bundleClassPath(): String {
        val list = mutableListOf(".")
        embeddableJars.onEach { jar -> list.add("${AemPlugin.OSGI_EMBED}/${jar.name}") }

        return list.joinToString(",")
    }

    private fun includeResource(): String {
        return embeddableJars.map { jar -> "${AemPlugin.OSGI_EMBED}/${jar.name}=${jar.path}" }.joinToString(",")
    }

    private fun includeServiceComponents() {
        if (config.scrEnabled && serviceComponents.isNotEmpty()) {
            addInstruction(SERVICE_COMPONENT_INSTRUCTION, { serviceComponentInstruction() })
        }
    }

    private fun serviceComponentInstruction(): String {
        project.logger.info("Including service components: ${serviceComponents.map { it.name }}")

        return serviceComponents.map { file -> "${AemPlugin.OSGI_INF}/${file.name}" }.joinToString(",")
    }

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

    private fun osgiPluginApplied() = project.plugins.hasPlugin(OSGI_PLUGIN_ID)

    private fun bundlePluginApplied() = project.plugins.hasPlugin(BUNDLE_PLUGIN_ID)

    @Suppress("unchecked_cast")
    private fun addInstruction(config: BundleExtension, name: String, value: String) {
        val instructions = config.instructions as Map<String, Any>
        if (!instructions.contains(name)) {
            if (!value.isNullOrBlank()) {
                config.instruction(name, value)
            }
        }
    }

}