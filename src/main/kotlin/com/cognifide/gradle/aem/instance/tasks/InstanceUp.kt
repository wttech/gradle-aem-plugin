package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstance() {

    private var awaitOptions: AwaitUpAction.() -> Unit = {
        unchanged { enabled.set(false) }
    }

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
        convention(true)
        aem.prop.boolean("instance.up.ensured")?.let { set(it) }
    }

    @TaskAction
    fun up() {
        localInstanceManager.base.examinePrerequisites(anyInstances)

        val upInstances = localInstanceManager.up(anyInstances, awaitOptions)

        if (ensured.get()) {
            val alreadyUpInstances = anyInstances - upInstances
            if (alreadyUpInstances.isNotEmpty()) {
                localInstanceManager.base.awaitUp(alreadyUpInstances, awaitOptions)
            }
        }

        localInstanceManager.customize(anyInstances)

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
