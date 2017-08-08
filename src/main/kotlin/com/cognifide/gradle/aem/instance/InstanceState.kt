package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.apache.commons.httpclient.params.HttpConnectionParams
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class InstanceState(val project: Project, val instance: Instance, val timeout: Int) {

    companion object {
        fun awaitStable(project: Project, instances: List<Instance> = Instance.locals(project)) {
            val config = AemConfig.of(project)
            val logger = project.logger

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
                val instanceProgress = instanceStates.map { progressFor(it, attempt) }.joinToString(" | ")

                progressLogger.progress(instanceProgress)

                if (instanceStates.all { it.stable }) {
                    logger.info("Instance(s) are now stable.")
                    return@waitUntil false
                }

                true
            })

            progressLogger.completed()
        }

        private fun progressFor(it: InstanceState, attempt: Long): String {
            return "${it.instance.name}: ${progressIndicator(it, attempt)} ${it.bundleState.stats} [${it.bundleState.stablePercent}]"
        }

        private fun progressIndicator(state: InstanceState, attempt: Long): String {
            return if (state.stable || attempt.rem(2) == 0L) {
                "*"
            } else {
                " "
            }
        }
    }

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val sync = InstanceSync(project, instance)

    var bundleStateParametrizer: (HttpConnectionParams) -> Unit = { params ->
        params.connectionTimeout = timeout
        params.soTimeout = timeout
    }

    val bundleState by lazy {
        sync.determineBundleState(bundleStateParametrizer)
    }

    val stable: Boolean
        get() = bundleState.stable

}