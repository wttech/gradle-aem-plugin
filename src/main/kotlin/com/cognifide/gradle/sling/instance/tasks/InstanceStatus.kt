package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.tasks.InstanceTask
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
            common.parallel.map(instances.get()) { instance ->
                increment("Instance '${instance.name}'") {
                    instance to instanceManager.statusReporter.report(instance)
                }
            }.sortedBy { it.first.name }.forEach { println(it.second) }
        }
    }

    init {
        description = "Prints status of Sling instances and installed packages."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
