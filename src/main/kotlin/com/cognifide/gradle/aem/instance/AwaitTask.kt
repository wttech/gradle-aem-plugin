package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class AwaitTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemAwait"
    }

    init {
        description = "Waits until all local AEM instance(s) be stable."
    }

    @TaskAction
    fun await() {
        val instances = Instance.filter(project)

        AwaitAction(project, instances).perform()
        notifier.default("Instance(s) stable and healthy", "Which: ${instances.names}")
    }

}