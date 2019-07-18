package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class PackageUninstall : PackageTask() {

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
        checkInstances()

        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.get(file)
                    packageManager.uninstall(pkg.path)
                }
            }
        }

        aem.notifier.notify("Package uninstalled", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "packageUninstall"
    }
}
