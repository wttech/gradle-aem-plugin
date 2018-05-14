package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

open class AwaitTask : SyncTask() {

    companion object {
        val NAME = "aemAwait"
    }

    init {
        description = "Waits until all local AEM instance(s) be stable."
    }

    @TaskAction
    fun await() {
        val instances = Instance.filter(project)
        AwaitAction(project, instances).perform()

        notifier.default(
                "Instance(s) stable and healthy",
                instances.joinToString(", ") { it.name }
        )
    }

}