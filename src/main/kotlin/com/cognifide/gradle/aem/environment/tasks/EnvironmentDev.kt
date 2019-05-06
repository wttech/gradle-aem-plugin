package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.EnvironmentException
import org.gradle.api.tasks.TaskAction

open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Watches for HTTPD configuration file changes and reloads service deployed on AEM virtualized environment"
    }

    @TaskAction
    fun dev() {
        with(aem) {
            progressLogger {
                logger.lifecycle("Watching for HTTPD configuration file changes in directory: ${environment.httpdConfDir}")

                // Whatever on parent logger to be able to pin children loggers from other threads
                progress("Watching files")

                fileWatcher {
                    dir = environment.httpdConfDir
                    onChange = { changes ->
                        logger.lifecycle("Reloading HTTP service due to file changes:\n${changes.joinToString("\n")}")

                        val restarted = environment.httpd.restart(false)
                        if (restarted) {
                            logger.lifecycle("Running environment health checks")

                            try {
                                environment.check()
                                logger.lifecycle("Configuration change passed environment checks")
                            } catch (e: EnvironmentException) {
                                logger.lifecycle("Configuration change caused environment checks fail(s)", e)
                            }
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