package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.instance.AemInstance
import com.cognifide.gradle.aem.instance.AemLocalHandler
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
            val localInstance = AemLocalHandler(sync.instance, project)

            logger.info("Destroying: $localInstance")
            localInstance.destroy()
            logger.info("Destroyed: $localInstance")
        }, AemInstance.filter(project, AemInstance.FILTER_LOCAL))
    }

}