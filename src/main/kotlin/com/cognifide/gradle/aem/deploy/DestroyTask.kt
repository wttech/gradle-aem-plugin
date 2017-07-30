package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class DestroyTask : SyncTask() {

    companion object {
        val NAME = "aemDestroy"
    }

    init {
        group = AemTask.GROUP
        description = "Destroys local AEM instance(s)."
    }

    @TaskAction
    fun destroy() {
        synchronizeLocalInstances { it.destroy() }
    }

}