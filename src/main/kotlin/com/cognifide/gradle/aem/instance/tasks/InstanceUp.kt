package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstanceTask() {

    private var awaitOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls instance awaiting.
     */
    fun await(options: AwaitUpAction.() -> Unit) {
        this.awaitOptions = options
    }

    /**
     * Ensure that already running instance are truly running (checking much more than status script only).
     */
    @Internal
    val ensured = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.up.ensured")?.let { set(it) }
    }

    @TaskAction
    fun up() {
        localInstanceManager.base.examinePrerequisites(instances.get())

        val upInstances = localInstanceManager.up(instances.get(), awaitOptions)

        if (ensured.get()) {
            val alreadyUpInstances = instances.get() - upInstances
            localInstanceManager.base.awaitUp(alreadyUpInstances, awaitOptions)
        }

        if (upInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) up", "Which: ${upInstances.names}")
        }
    }

    init {
        description = "Turns on local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
