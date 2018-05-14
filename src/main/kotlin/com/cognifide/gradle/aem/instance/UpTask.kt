package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

open class UpTask : SyncTask() {

    companion object {
        val NAME = "aemUp"
    }

    init {
        description = "Turns on local AEM instance(s)."
    }

    @TaskAction
    fun up() {
        val instances = Instance.locals(project)

        synchronizeLocalInstances(instances, { it.up() })
        AwaitAction(project, instances).perform()
        synchronizeLocalInstances(instances, { it.init() })

        notifier.default(
                "Instance(s) up and ready",
                instances.joinToString(", ") { it.name }
        )
    }

}