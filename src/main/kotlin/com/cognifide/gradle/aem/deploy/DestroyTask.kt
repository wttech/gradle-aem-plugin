package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemLocalInstance
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
    fun create() {
        synchronize({ sync ->
            val localInstance = AemLocalInstance(sync.instance, project)

            logger.info("Destroying: $localInstance")
            localInstance.destroy()
        })
    }

}