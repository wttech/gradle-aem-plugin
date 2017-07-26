package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.instance.AemLocalHandler
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class UpTask : SyncTask() {

    companion object {
        val NAME = "aemUp"
    }

    init {
        group = AemTask.GROUP
        description = "Turns on local AEM instance(s)."
    }

    @TaskAction
    fun up() {
        synchronize({ sync ->
            val localInstance = AemLocalHandler(sync.instance, project)

            logger.info("Turning on: $localInstance")
            localInstance.up()
            logger.info("Turned on: $localInstance")
        })
    }

}