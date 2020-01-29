package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.common.CommonException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Debug : AemDefaultTask() {

    @Internal
    val file = common.temporaryFile("$name/debug.json")

    /**
     * Dump package states on defined instances.
     */
    @Internal
    var packageDeployed: Boolean = aem.prop.boolean("debug.packageDeployed") ?: !aem.offline

    @get:Internal
    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "buildInfo" to buildProperties,
                    "projectInfo" to projectProperties,
                    "packageDeployed" to packageProperties
            )
        }

    @get:Internal
    val buildProperties: Map<String, Any>
        get() = mapOf(
                "plugin" to AemPlugin.BUILD,
                "gradle" to mapOf(
                        "version" to project.gradle.gradleVersion,
                        "homeDir" to project.gradle.gradleHomeDir
                ),
                "java" to mapOf(
                        "version" to System.getProperty("java.specification.version"),
                        "homeDir" to System.getProperty("java.home")
                )
        )

    @get:Internal
    val projectProperties: Map<String, String>
        get() = mapOf(
                "displayName" to project.displayName,
                "path" to project.path,
                "name" to project.name,
                "dir" to project.projectDir.absolutePath
        )

    @get:Internal
    val packageProperties: Map<String, Package?>
        get() = if (!project.plugins.hasPlugin(PackagePlugin.ID) || !packageDeployed || aem.instances.isEmpty()) {
            mapOf()
        } else {
            logger.info("Determining package states on instances: ${aem.instances.names}")

            mutableMapOf<String, Package?>().apply {
                aem.syncPackages { pkg ->
                    try {
                        put(instance.name, packageManager.find(pkg))
                    } catch (e: CommonException) {
                        logger.info("Cannot determine remote package, because instance is not available: $instance")
                        logger.debug("Detailed error", e)
                    }
                }
            }
        }

    @TaskAction
    fun doDebug() {
        logger.lifecycle("Dumping AEM build configuration of $project to file: $file")

        val json = Formats.toJson(properties, true)
        file.apply {
            parentFile.mkdirs()
            bufferedWriter().use { it.write(json) }
        }
        logger.info(json)

        common.notifier.notify("AEM configuration dumped", "For $project to file: ${Formats.projectPath(file, project)}")
    }

    init {
        description = "Dumps effective AEM build configuration of project to JSON file"
    }

    companion object {
        const val NAME = "debug"

        const val NAME_FALLBACK = "debugAem"
    }
}
