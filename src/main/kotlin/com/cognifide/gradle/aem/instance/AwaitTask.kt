package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

open class AwaitTask : SyncTask() {

    companion object {
        val NAME = "aemAwait"
    }

    init {
        group = AemTask.GROUP
        description = "Waits until all local AEM instance(s) be stable."
    }

    @TaskAction
    fun await() {
        awaitStableInstances()
    }

}