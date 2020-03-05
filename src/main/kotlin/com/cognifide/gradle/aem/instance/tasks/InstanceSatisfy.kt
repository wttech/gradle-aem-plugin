package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceSatisfy : InstanceTask() {

    @TaskAction
    fun satisfy() {
        instanceManager.examine(instances.get())

        val packageActions = instanceManager.satisfier.satisfy(instances.get())
        if (packageActions.isNotEmpty()) {
            val packages = packageActions.map { it.pkg }.toSet()
            val packageInstances = packageActions.map { it.instance }.toSet()

            if (packages.size == 1) {
                common.notifier.notify("Package satisfied", "${packages.first().name} on ${packageInstances.names}")
            } else {
                common.notifier.notify("Packages satisfied", "Performed ${packageActions.size} action(s) for " +
                        "${packages.size} package(s) on ${packageInstances.size} instance(s).")
            }
        }
    }

    init {
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."
    }

    companion object {
        const val NAME = "instanceSatisfy"
    }
}
