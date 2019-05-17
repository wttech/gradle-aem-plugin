package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class InstanceCreateOnly : LocalInstanceTask() {

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

        create(uncreatedInstances)

        val createdInstances = uncreatedInstances.filter { it.created }

        aem.notifier.notify("Instance(s) created", "Which: ${createdInstances.names}")
    }

    private fun create(uncreatedInstances: List<LocalInstance>) {
        if (instanceOptions.jar == null || instanceOptions.license == null) {
            throw InstanceException("Cannot create instances due to lacking source files. " +
                    "Ensure having specified: local instance ZIP url or jar & license url.")
        }

        aem.progress(uncreatedInstances.size) {
            uncreatedInstances.onEachApply {
                increment("Creating instance '$name'") {
                    create()
                }
            }
        }
    }

    companion object {
        const val NAME = "instanceCreateOnly"
    }
}