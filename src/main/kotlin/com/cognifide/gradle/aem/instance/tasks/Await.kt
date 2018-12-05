package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class Await : AemDefaultTask() {

    companion object {
        const val NAME = "aemAwait"
    }

    init {
        description = "Waits until all local AEM instance(s) be stable."
    }

    private var options: AwaitAction.() -> Unit = {}

    fun options(options: AwaitAction.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun await() {
        aem.actions.await(options)
    }
}