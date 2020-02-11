package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.common.utils.onEachApply
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstanceTask() {

    private var awaitOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls instance awaiting.
     */
    fun await(options: AwaitUpAction.() -> Unit) {
        this.awaitOptions = options
    }

    @TaskAction
    fun up() {
        val downInstances = instances.filter { !it.running }
        if (downInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to turn on")
            return
        }

        common.progress(downInstances.size) {
            downInstances.onEachApply {
                increment("Customizing instance '$name'") { customize() }
            }
        }

        common.progress(downInstances.size) {
            common.parallel.with(downInstances) {
                increment("Starting instance '$name'") { up() }
            }
        }

        aem.instanceActions.awaitUp {
            instances.convention(downInstances)
            awaitOptions()
        }

        common.progress(downInstances.size) {
            common.parallel.with(downInstances) {
                increment("Initializing instance '$name'") { init() }
            }
        }

        common.notifier.lifecycle("Instance(s) up", "Which: ${downInstances.names}")
    }

    init {
        description = "Turns on local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
