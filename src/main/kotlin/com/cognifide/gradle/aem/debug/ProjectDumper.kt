package com.cognifide.gradle.aem.debug

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.deploy.ListResponse
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class ProjectDumper(@Transient val project: Project) {

    val logger: Logger = project.logger

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "projectInfo" to projectProperties,
                    "packageProperties" to PropertyParser(project).aemProperties,
                    "packageDeployed" to packageProperties
            )
        }

    val projectProperties: Map<String, String>
        get() = mapOf(
                "displayName" to project.displayName,
                "path" to project.path,
                "name" to project.name,
                "dir" to project.projectDir.absolutePath
        )

    val packageProperties: Map<String, ListResponse.Package?>
        get() = if (PropertyParser(project).checkOffline()) {
            logger.info("Skipping determining deployed packages due to offline mode.")
            mapOf()
        } else {
            AemConfig.of(project).instances.mapValues {
                val instance = it.value
                try {
                    InstanceSync(project, instance).determineRemotePackage()
                } catch (e: Exception) {
                    logger.info("Cannot determine remote package, because instance is not available: $instance")
                    logger.debug("Detailed error", e)
                    null
                }
            }
        }
}