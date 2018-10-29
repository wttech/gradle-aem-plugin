package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.TaskAction

open class UninstallTask : SyncTask() {

    init {
        description = "Uninstalls AEM package on instance(s)."

        afterConfigured { aem.props.checkForce() }
    }

    @TaskAction
    fun uninstall() {
        aem.syncPackages(instances, packages) { uninstallPackage(determineRemotePackagePath(it)) }

        aem.notifier.default("Package uninstalled", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "aemUninstall"
    }

}
