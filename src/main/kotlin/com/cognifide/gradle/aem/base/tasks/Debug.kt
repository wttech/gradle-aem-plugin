package com.cognifide.gradle.aem.base.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class Debug : AemDefaultTask() {

    @OutputFile
    val file = AemTask.temporaryFile(project, NAME, "debug.json")

    /**
     * Dump package states on defined instances.
     */
    @Input
    var packageDeployed: Boolean = aem.props.boolean("aem.debug.packageDeployed") ?: !project.gradle.startParameter.isOffline

    init {
        description = "Dumps effective AEM build configuration of project to JSON file"

        outputs.upToDateWhen { false }
    }

    @get:Internal
    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "buildInfo" to buildProperties,
                    "projectInfo" to projectProperties,
                    "baseConfig" to aem.config,
                    "bundleConfig" to aem.bundles,
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
                        put(instance.name, determineRemotePackage(pkg))
                    } catch (e: AemException) {
                        logger.info("Cannot determine remote package, because instance is not available: $instance")
                        logger.debug("Detailed error", e)
                    }
                }
            }
        }

    @TaskAction
    fun debug() {
        logger.lifecycle("Dumping AEM build configuration of $project to file: $file")

        val json = Formats.toJson(properties)

        file.bufferedWriter().use { it.write(json) }
        logger.info(json)

        aem.notifier.notify("Configuration dumped", "For $project to file: ${Formats.projectPath(file, project)}")
    }

    companion object {
        const val NAME = "aemDebug"
    }
}
