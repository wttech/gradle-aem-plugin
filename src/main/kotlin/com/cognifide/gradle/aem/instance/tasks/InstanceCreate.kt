package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceCreate : LocalInstanceTask() {

    init {
        description = "Creates local AEM instance(s)."
    }

    @TaskAction
    fun create() {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.info("No instance(s) to create")
            return
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")

        val backupZip = manager.backupZip
        if (backupZip != null) {
            if (instances != uncreatedInstances) {
                throw InstanceException("Backup ZIP cannot be used to create missing instances.")
            }

            manager.createFromBackup(uncreatedInstances, backupZip)
        } else {
            manager.createFromScratch(uncreatedInstances)
        }

        val createdInstances = uncreatedInstances.filter { it.created }

        aem.notifier.notify("Instance(s) created", "Which: ${createdInstances.names}")
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}