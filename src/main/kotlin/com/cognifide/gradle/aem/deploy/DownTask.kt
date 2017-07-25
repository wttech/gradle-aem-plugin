package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemLocalInstance
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class DownTask : SyncTask() {

    companion object {
        val NAME = "aemDown"
    }

    init {
        group = AemTask.GROUP
        description = "Turns off local AEM instance(s)."
    }

    @TaskAction
    fun down() {
        synchronize({ sync ->
            val localInstance = AemLocalInstance(sync.instance, project)

            logger.info("Turning off: $localInstance")
            localInstance.down()
            logger.info("Turned off: $localInstance")
        })
    }

}