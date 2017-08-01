package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.SyncTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.tasks.TaskAction

open class AwaitTask : SyncTask() {

    init {
        group = AemTask.GROUP
        description = "Waits until all local AEM instance(s) be stable."
    }

    @TaskAction
    fun await() {
        awaitStableInstances(filterInstances())
    }

    private fun awaitStableInstances(instances: List<Instance>) {
        if (config.instanceAwaitDelay > 0) {
            logger.info("Delaying instance stability checking")
            Behaviors.waitFor(config.instanceAwaitDelay)
        }

        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

        Behaviors.waitUntil(config.instanceAwaitInterval, { elapsed, attempt ->
            if (config.instanceAwaitTimeout > 0 && elapsed > config.instanceAwaitTimeout) {
                logger.warn("Timeout reached while awaiting stable instance(s)")
                return@waitUntil false
            }

            val instanceStates = instances.map { InstanceState(project, it, config.instanceAwaitTimeout) }
            val instanceProgress = instanceStates.map { instanceStateMessage(it, attempt) }.joinToString(" | ")

            progressLogger.progress(instanceProgress)

            if (instanceStates.all { it.stable }) {
                logger.info("Instance(s) are now stable.")
                return@waitUntil false
            }

            true
        })

        progressLogger.completed()
    }

    private fun instanceStateMessage(it: InstanceState, attempt: Long): String {
        return "${it.instance.name}: ${instanceProgressIndicator(it, attempt)} ${it.bundleState.stats} [${it.bundleState.stablePercent}]"
    }

    private fun instanceProgressIndicator(state: InstanceState, attempt: Long): String {
        return if (state.stable || attempt.rem(2) == 0L) {
            "*"
        } else {
            " "
        }
    }

}