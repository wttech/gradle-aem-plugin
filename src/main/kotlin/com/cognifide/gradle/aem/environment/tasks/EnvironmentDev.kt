package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.gradle.api.tasks.TaskAction

@UseExperimental(ObsoleteCoroutinesApi::class)
open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Listens for HTTPD configuration file changes and reloads service deployed on AEM virtualized environment"
    }

    @TaskAction
    fun dev() {
        with(aem) {
            logger.lifecycle("Listening for HTTPD configuration file changes in directory: ${environment.httpdConfDir}")

            fileWatcher {
                dir = environment.httpdConfDir
                onChange = { changes ->
                    logger.lifecycle("Reloading HTTP service due to file changes:\n${changes.joinToString("\n")}")

                    val restarted = environment.httpd.restart(false)
                    if (restarted) {
                        logger.lifecycle("Checking HTTP health checks")
                        val unavailableServices = environment.healthChecker.findUnavailable()
                        if (unavailableServices.isEmpty()) {
                            logger.lifecycle("All stable, configuration update looks good.")
                        } else {
                            logger.lifecycle("Services verification failed! URLs are unavailable or returned different response than expected:" +
                                    "\n${unavailableServices.joinToString("\n")}" +
                                    "\nFix configuration to make it working again.")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val NAME = "environmentDev"
    }
}