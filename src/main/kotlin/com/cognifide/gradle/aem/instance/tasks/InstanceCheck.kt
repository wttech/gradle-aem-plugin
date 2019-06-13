package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import org.gradle.api.tasks.TaskAction

open class InstanceCheck : AemDefaultTask() {

    init {
        description = "Check health condition of all instances."
    }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun check() {
        aem.instanceActions.awaitUp(awaitUpOptions)
    }

    companion object {
        const val NAME = "instanceCheck"
    }
}