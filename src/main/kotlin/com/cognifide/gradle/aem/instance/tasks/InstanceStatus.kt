package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.TaskAction

open class InstanceStatus : Instance() {

    @Suppress("MagicNumber")
    @TaskAction
    fun status() {
        common.progress(anyInstances.size) {
            step = "Initializing"
            instanceManager.statusReporter.init()

            step = "Checking statuses"
            common.parallel.map(anyInstances) { instance ->
                increment("Instance '${instance.name}'") {
                    instance to instanceManager.statusReporter.report(instance)
                }
            }.sortedBy { it.first.name }.forEach { println(it.second) }
        }
    }

    init {
        description = "Prints status of AEM instances and installed packages."
    }

    companion object {
        const val NAME = "instanceStatus"
    }
}
