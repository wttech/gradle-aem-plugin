package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.BundleSynchronizer
import org.gradle.api.tasks.TaskAction
import org.osgi.framework.Bundle

open class AwaitTask : SyncTask() {

    init {
        group = AemTask.GROUP
        description = "Waits until all OSGi bundles deployed on local AEM instance(s) be active."
    }

    @TaskAction
    fun await() {
        logger.info("Awaiting all OSGi bundles active...")
        Behaviors.waitUntil({ attempt, attempts ->
            logger.info("Attempt [$attempt/$attempts]")

            filterInstances().all { instance ->
                val inactiveBundles = BundleSynchronizer(project, instance).all.filter { it.state != Bundle.ACTIVE }
                if (inactiveBundles.isNotEmpty()) {
                    logger.info("Inactive bundles found (${inactiveBundles.size}) on $instance")
                }

                inactiveBundles.isEmpty()
            }
        })
    }

}