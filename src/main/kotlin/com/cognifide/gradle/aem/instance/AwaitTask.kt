package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.BundleRepository
import org.gradle.api.tasks.TaskAction

// TODO use progressLogger instead of messing logs ;)
open class AwaitTask : SyncTask() {

    init {
        group = AemTask.GROUP
        description = "Waits until all OSGi bundles deployed on local AEM instance(s) be active."
    }

    @TaskAction
    fun await() {
        logger.info("Awaiting all OSGi bundles active")
        Behaviors.waitUntil({ attempt, attempts ->
            logger.debug("Attempt [$attempt/$attempts]")

            filterInstances().any { instance ->
                val state = BundleRepository(project, DeploySynchronizer(instance, config)).ask()
                if (state == null) {
                    logger.info("Cannot check bundles state on $instance")
                    return@any true
                }

                if (!state.stable) {
                    logger.info("Unstable state detected on $instance: ${state.status}")
                    return@any true
                }

                false
            }
        })
    }

}