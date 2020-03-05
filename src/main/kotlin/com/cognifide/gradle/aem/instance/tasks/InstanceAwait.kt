package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceAwait : InstanceTask() {

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun await() {
        instanceManager.examineRunningOther(instances.get())
        instanceManager.awaitUp(instances.get(), awaitUpOptions)
    }

    init {
        description = "Await for healthy condition of all AEM instances."
    }

    companion object {
        const val NAME = "instanceAwait"
    }
}
