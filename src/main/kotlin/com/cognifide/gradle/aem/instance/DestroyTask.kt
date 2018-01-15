package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

open class DestroyTask : SyncTask() {

    companion object {
        val NAME = "aemDestroy"
    }

    init {
        description = "Destroys local AEM instance(s)."
    }

    @TaskAction
    fun destroy() {
        propertyParser.checkForce()

        synchronizeLocalInstances { it.destroy() }
    }

}