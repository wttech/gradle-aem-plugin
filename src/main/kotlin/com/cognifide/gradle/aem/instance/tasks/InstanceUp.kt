package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.instance.action.AwaitAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstanceTask() {

    init {
        description = "Turns on local AEM instance(s)."
    }

    private var initOptions: LocalInstance.() -> Unit = {}

    private var awaitOptions: AwaitAction.() -> Unit = {}

    /**
     * Hook called only when instance is up first time.
     */
    fun init(options: LocalInstance.() -> Unit) {
        this.initOptions = options
    }

    /**
     * Controls instance awaiting.
     */
    fun await(options: AwaitAction.() -> Unit) {
        this.awaitOptions = options
    }

    @TaskAction
    fun up() {
        aem.parallel.with(instances) { up() }

        aem.instanceActions.await {
            instances = this@InstanceUp.instances
            awaitOptions()
        }

        aem.parallel.with(instances) { init(initOptions) }

        aem.notifier.notify("Instance(s) up", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceUp"
    }
}