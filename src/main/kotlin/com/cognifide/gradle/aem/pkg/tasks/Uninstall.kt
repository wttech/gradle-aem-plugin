package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.fileNames
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Uninstall : Sync() {

    init {
        description = "Uninstalls AEM package on instance(s)."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.props.checkForce()
        }
    }

    @TaskAction
    fun uninstall() {
        aem.progress({
            header = "Uninstalling package(s) on instance(s)"
            total = instances.size.toLong() * packages.size.toLong()
        }, {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    uninstallPackage(determineRemotePackagePath(pkg))
                }
            }
        })

        aem.notifier.notify("Package uninstalled", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "aemUninstall"
    }
}
