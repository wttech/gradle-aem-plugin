package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.common.utils.onEachApply
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstanceTask() {

    init {
        description = "Turns on local AEM instance(s)."
    }

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
            logger.info("No instance(s) to turn on")
            return
        }

        aem.progress(downInstances.size) {
            downInstances.onEachApply {
                increment("Customizing instance '$name'") { customize() }
            }
        }

        aem.progress(downInstances.size) {
            aem.parallel.with(downInstances) {
                increment("Starting instance '$name'") { up() }
            }
        }

        aem.instanceActions.awaitUp {
            instances = downInstances
            awaitOptions()
        }

        aem.progress(downInstances.size) {
            aem.parallel.with(downInstances) {
                increment("Initializing instance '$name'") { init() }
            }
        }

        aem.notifier.notify("Instance(s) up", "Which: ${downInstances.names}")
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
