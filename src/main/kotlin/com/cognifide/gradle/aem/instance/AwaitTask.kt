package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.BundleRepository
import org.gradle.api.tasks.TaskAction
import org.osgi.framework.Bundle

open class AwaitTask : SyncTask() {

    init {
        group = AemTask.GROUP
        description = "Waits until all OSGi bundles deployed on local AEM instance(s) be active."
    }

    @TaskAction
    fun await() {
        logger.info("Awaiting all OSGi bundles active")
        Behaviors.waitUntil({ attempt, attempts ->
            logger.info("Attempt [$attempt/$attempts]")

            filterInstances().any { instance ->
                try {
                    val response = BundleRepository(project, DeploySynchronizer(instance, config)).ask()
                    val inactiveBundles = response.bundles.filter { it.state != Bundle.ACTIVE }
                    val wait = inactiveBundles.isNotEmpty()
                    if (wait) {
                        logger.info("Inactive bundles found (${inactiveBundles.size}) on $instance")
                    }

                    wait
                } catch (e: Exception) {
                    logger.warn("Cannot check bundles on $instance")
                    logger.debug("Reason", e)

                    false
                }

            }
        })
    }

}