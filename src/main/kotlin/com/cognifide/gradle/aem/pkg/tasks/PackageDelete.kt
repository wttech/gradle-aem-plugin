package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class PackageDelete : PackageTask() {

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
        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.getPackage(file)
                    packageManager.deletePackage(pkg.path)
                }
            }
        }

        aem.notifier.notify("Package deleted", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
