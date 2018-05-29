package com.cognifide.gradle.aem.base.debug

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.deploy.ListResponse
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class ProjectDumper(@Transient val project: Project) {

    val logger: Logger = project.logger

    val props = PropertyParser(project)

    val config = AemConfig.of(project)

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "buildInfo" to buildProperties,
                    "projectInfo" to projectProperties,
                    "packageProperties" to props.packageProps,
                    "packageDeployed" to packageProperties
            )
        }

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

    val projectProperties: Map<String, String>
        get() = mapOf(
                "displayName" to project.displayName,
                "path" to project.path,
                "name" to project.name,
                "dir" to project.projectDir.absolutePath
        )

    val packageProperties: Map<String, ListResponse.Package?>
        get() = if (config.debugPackageDeployed || config.instances.isEmpty()) {
            mapOf()
        } else {
            logger.info("Determining package states on instances: ${config.instances.values.names}")

            mutableMapOf<String, ListResponse.Package?>().apply {
                config.instances.entries.parallelStream().forEach { (name, instance) ->
                    try {
                        put(name, InstanceSync(project, instance).determineRemotePackage())
                    } catch (e: Exception) {
                        logger.info("Cannot determine remote package, because instance is not available: $instance")
                        logger.debug("Detailed error", e)
                    }
                }
            }
        }
}