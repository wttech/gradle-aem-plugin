package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class InstanceAwait : AemDefaultTask() {

    init {
        description = "Waits until all local AEM instance(s) be stable."
    }

    private var options: AwaitAction.() -> Unit = {}

    fun options(options: AwaitAction.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun await() {
        aem.instanceActions.await(options)
    }

    companion object {
        const val NAME = "instanceAwait"
    }
}