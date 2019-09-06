package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
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
        aem.progress(instances.size) {
            aem.parallel.with(instances) {
                increment("Starting instance '$name'") { up() }
            }
        }

        aem.instanceActions.awaitUp {
            instances = this@InstanceUp.instances
            awaitOptions()
        }

        aem.progress(instances.size) {
            aem.parallel.with(instances) {
                increment("Initializing instance '$name'") { init() }
            }
        }

        aem.notifier.notify("Instance(s) up", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
