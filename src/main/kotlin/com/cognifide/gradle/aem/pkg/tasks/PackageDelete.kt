package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class PackageDelete : PackageTask() {

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.prop.checkForce(this)
        }
    }

    @TaskAction
    fun delete() {
        instances.checkAvailable()

        aem.progress(instances.size * packages.size) {
            aem.syncFiles(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.get(file)
                    packageManager.delete(pkg.path)
                }
            }
        }

        aem.notifier.notify("Package deleted", "${packages.fileNames} on ${instances.names}")
    }

    init {
        description = "Deletes AEM package on instance(s)."
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
