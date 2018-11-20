package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Delete : Sync() {

    init {
        description = "Deletes AEM package on instance(s)."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.props.checkForce()
        }
    }

    @TaskAction
    fun delete() {
        aem.syncPackages(instances, packages) { deletePackage(determineRemotePackagePath(it)) }

        aem.notifier.notify("Package deleted", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemDelete"
    }

}
