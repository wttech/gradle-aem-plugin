package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.instance.AemInstance
import com.cognifide.gradle.aem.instance.AemLocalHandler
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
        synchronize({ AemLocalHandler(project, it.instance).destroy() }, AemInstance.filter(project, AemInstance.FILTER_LOCAL))
    }

}