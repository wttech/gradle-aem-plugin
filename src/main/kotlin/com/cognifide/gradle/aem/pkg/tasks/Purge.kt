package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.fileNames
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Purge : PackageTask() {

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.props.checkForce()
        }
    }

    @TaskAction
    fun purge() {
        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    try {
                        val pkg = getPackage(file)

                        uninstall(this, pkg.path)
                        delete(this, pkg.path)
                    } catch (e: InstanceException) {
                        aem.logger.info(e.message)
                        aem.logger.debug("Nothing to purge.", e)
                    }
                }
            }
        }

        aem.notifier.notify("Package purged", "${packages.fileNames} from ${instances.names}")
    }

    private fun uninstall(sync: InstanceSync, packagePath: String) {
        try {
            sync.uninstallPackage(packagePath)
        } catch (e: InstanceException) {
            logger.info("${e.message} Is it installed already?")
            logger.debug("Cannot uninstall package.", e)
        }
    }

    private fun delete(sync: InstanceSync, packagePath: String) {
        try {
            sync.deletePackage(packagePath)
        } catch (e: InstanceException) {
            logger.info(e.message)
            logger.debug("Cannot delete package.", e)
        }
    }

    companion object {
        const val NAME = "aemPurge"
    }
}
