package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class PurgeTask : AemDefaultTask() {

    companion object {
        val NAME = "aemPurge"
    }

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."

        afterConfigured { props.checkForce() }
    }

    @TaskAction
    fun purge() {
        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        instances.sync(project) { sync ->
            try {
                val packagePath = sync.determineRemotePackagePath()

                uninstall(packagePath, sync)
                delete(packagePath, sync)
            } catch (e: DeployException) {
                logger.info(e.message)
                logger.debug("Nothing to purge.", e)
            }
        }

        notifier.default("Package purged", "$pkg from ${instances.names}")
    }

    private fun uninstall(packagePath: String, sync: InstanceSync) {
        try {
            sync.uninstallPackage(packagePath)
        } catch (e: DeployException) {
            logger.info("${e.message} Is it installed already?")
            logger.debug("Cannot uninstall package.", e)
        }
    }

    private fun delete(packagePath: String, sync: InstanceSync) {
        try {
            sync.deletePackage(packagePath)
        } catch (e: DeployException) {
            logger.info(e.message)
            logger.debug("Cannot delete package.", e)
        }
    }

}
