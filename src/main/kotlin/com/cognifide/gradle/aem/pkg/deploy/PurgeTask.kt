package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import org.gradle.api.tasks.TaskAction

open class PurgeTask : SyncTask() {

    companion object {
        val NAME = "aemPurge"
    }

    init {
        group = AemTask.GROUP
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."
    }

    @TaskAction
    fun purge() {
        propertyParser.checkForce()

        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        synchronizeInstances(instances, { sync ->
            try {
                val packagePath = sync.determineRemotePackagePath()

                uninstall(packagePath, sync)
                delete(packagePath, sync)
            } catch (e: DeployException) {
                logger.info(e.message)
                logger.debug("Nothing to purge.", e)
            }
        })

        notifier.default("Package purged", "$pkg on ${instances.joinToString(", ") { it.name }}")
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
