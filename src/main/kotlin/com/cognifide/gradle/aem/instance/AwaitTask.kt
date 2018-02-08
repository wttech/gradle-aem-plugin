package com.cognifide.gradle.aem.instance

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
        awaitStableInstances()
    }

}