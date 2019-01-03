package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class Up : Instance() {

    init {
        description = "Turns on local AEM instance(s)."
    }

    private var initOptions: LocalHandle.() -> Unit = {}

    private var awaitOptions: AwaitAction.() -> Unit = {}

    /**
     * Hook called only when instance is up first time.
     */
    fun init(options: LocalHandle.() -> Unit) {
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
        aem.parallelWith(localHandles) { up() }

        aem.actions.await {
            instances = this@Up.instances
            awaitOptions()
        }

        aem.parallelWith(localHandles) { init(initOptions) }

        aem.notifier.notify("Instance(s) up", "Which: ${localHandles.names}")
    }

    companion object {
        const val NAME = "aemUp"
    }
}