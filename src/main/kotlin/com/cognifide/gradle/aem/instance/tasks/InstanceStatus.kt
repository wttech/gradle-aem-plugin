package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.StatusReporter
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.common.utils.using
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceStatus : InstanceTask() {

    @Internal
    val reporter = StatusReporter(aem)

    fun reporter(options: StatusReporter.() -> Unit) = reporter.using(options)

    @Suppress("MagicNumber")
    @TaskAction
    fun status() {
        if (instances.get().isEmpty()) {
            println("No instances defined!")
            return
        }

        common.progress(instances.get().size) {
            step = "Initializing"
            reporter.init()

            step = "Checking statuses"
            common.parallel.each(instances.get()) { instance ->
                increment("Instance '${instance.name}'") {
                    println(reporter.report(instance))
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
