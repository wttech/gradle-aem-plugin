package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.action.AwaitUpAction
import com.cognifide.gradle.sling.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceAwait : InstanceTask() {

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun await() {
        instanceManager.examinePrerequisites(instances.get())
        instanceManager.awaitUp(instances.get(), awaitUpOptions)
    }

    init {
        description = "Await for healthy condition of all Sling instances."
    }

    companion object {
        const val NAME = "instanceAwait"
    }
}
