package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class PackageDelete : PackageTask() {

    @TaskAction
    fun delete() {
        instances.checkAvailable()
        sync { packageManager.delete(it) }
        aem.notifier.notify("Package deleted", "${packages.fileNames} on ${instances.names}")
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.prop.checkForce(this)
        }
    }

    init {
        description = "Deletes AEM package on instance(s)."
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
