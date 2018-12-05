package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.AwaitAction
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Await : AemDefaultTask() {

    companion object {
        const val NAME = "aemAwait"
    }

    init {
        description = "Waits until all local AEM instance(s) be stable."
    }

    @Nested
    var await = AwaitAction(aem)

    fun options(configurer: AwaitAction.() -> Unit) {
        await.apply(configurer)
    }

    @TaskAction
    fun await() {
        await.perform()
    }
}