package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class DeleteTask : SyncTask() {

    companion object {
        val NAME = "aemDelete"
    }

    init {
        group = AemTask.GROUP
        description = "Deletes AEM package on instance(s)."
    }

    @TaskAction
    fun delete() {
        synchronize({ sync ->
            propertyParser.checkForce()

            deletePackage(determineRemotePackagePath(sync), sync)
        })
    }

}
