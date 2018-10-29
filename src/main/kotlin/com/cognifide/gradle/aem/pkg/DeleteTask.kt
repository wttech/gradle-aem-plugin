package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.TaskAction

open class DeleteTask : SyncTask() {

    init {
        description = "Deletes AEM package on instance(s)."

        afterConfigured { aem.props.checkForce() }
    }

    @TaskAction
    fun delete() {
        aem.syncPackages(instances, packages) { deletePackage(determineRemotePackagePath(it)) }

        aem.notifier.default("Package deleted", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemDelete"
    }

}
