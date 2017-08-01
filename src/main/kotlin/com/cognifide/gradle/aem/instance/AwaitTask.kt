package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.tasks.TaskAction

open class AwaitTask : SyncTask() {

    init {
        group = AemTask.GROUP
        description = "Waits until all local AEM instance(s) will be stable."
    }

    @TaskAction
    fun await() {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")
        val instances = filterInstances()

        progressLogger.started()

        Behaviors.waitUntil({ attempt, attempts ->
            logger.debug("Await attempt [$attempt/$attempts]")

            val instanceStates = instances.map { AemInstanceState(it, BundleRepository(project, DeploySynchronizer(it, config)).ask()) }
            val progress = instanceStates.map { "${it.instance.name}: ${it.bundleState.counts} [${it.bundleState.stablePercent}]" }.joinToString(" | ")

            progressLogger.progress(progress)
            instanceStates.any { !it.stable }
        }, {
            logger.error("Unstable instance(s) state, but timeout occurred.")
        }, config.instanceAwaitInterval)

        progressLogger.completed()
    }

}