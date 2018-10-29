package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.TaskAction

open class ActivateTask : SyncTask() {

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        aem.syncPackages(instances, packages) { activatePackage(determineRemotePackagePath(it)) }

        aem.notifier.default("Package activated", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemActivate"
    }

}