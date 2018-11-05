package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.TaskAction

open class PurgeTask : SyncTask() {

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."

        afterConfigured { aem.props.checkForce() }
    }

    @TaskAction
    fun purge() {
        aem.syncPackages(instances, packages) { pkg ->
            try {
                val packagePath = determineRemotePackagePath(pkg)

                uninstall(this, packagePath)
                delete(this, packagePath)
            } catch (e: DeployException) {
                logger.info(e.message)
                logger.debug("Nothing to purge.", e)
            }
        }

        aem.notifier.notify("Package purged", "${packages.fileNames} from ${instances.names}")
    }

    private fun uninstall(sync: InstanceSync, packagePath: String) {
        try {
            sync.uninstallPackage(packagePath)
        } catch (e: DeployException) {
            logger.info("${e.message} Is it installed already?")
            logger.debug("Cannot uninstall package.", e)
        }
    }

    private fun delete(sync: InstanceSync, packagePath: String) {
        try {
            sync.deletePackage(packagePath)
        } catch (e: DeployException) {
            logger.info(e.message)
            logger.debug("Cannot delete package.", e)
        }
    }

    companion object {
        const val NAME = "aemPurge"
    }

}
