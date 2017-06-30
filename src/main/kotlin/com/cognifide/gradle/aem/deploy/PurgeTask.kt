package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class PurgeTask : SyncTask() {

    companion object {
        val NAME = "aemPurge"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Uninstalls and then deletes AEM package on instance(s)."
    }

    @TaskAction
    fun purge() {
        synchronize({ sync ->
            propertyParser.checkForce()

            try {
                val packagePath = determineRemotePackagePath(sync)

                uninstall(packagePath, sync)
                delete(packagePath, sync)
            } catch (e: DeployException) {
                logger.info(e.message)
                logger.debug("Nothing to purge.", e)
            }
        })
    }

    private fun uninstall(packagePath: String, sync: DeploySynchronizer) {
        try {
            uninstallPackage(packagePath, sync)
        } catch(e: DeployException) {
            logger.info("${e.message} Is it installed already?")
            logger.debug("Cannot uninstall package.", e)
        }
    }

    private fun delete(packagePath: String, sync: DeploySynchronizer) {
        try {
            deletePackage(packagePath, sync)
        } catch (e: DeployException) {
            logger.info(e.message)
            logger.debug("Cannot delete package.", e)
        }
    }

}
