package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceStatus : InstanceTask() {

    @Suppress("MagicNumber")
    @TaskAction
    fun status() {
        if (instances.get().isEmpty()) {
            println("No instances defined!")
            return
        }

        common.progress(instances.get().size) {
            step = "Initializing"
            instanceManager.statusReporter.init()

            step = "Checking statuses"
            common.parallel.each(instances.get()) { instance ->
                increment("Instance '${instance.name}'") {
                    println(instanceManager.statusReporter.report(instance))
                }
            }
        }
    }

    init {
        description = "Prints status of AEM instances and installed packages."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
