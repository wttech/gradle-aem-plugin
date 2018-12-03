package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Purge : Sync() {

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
        aem.progress({
            header = "Purging package(s) from instance(s)"
            total = instances.size.toLong() * packages.size.toLong()
        }, {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    try {
                        val packagePath = determineRemotePackagePath(pkg)

                        uninstall(this, packagePath)
                        delete(this, packagePath)
                    } catch (e: InstanceException) {
                        logger.info(e.message)
                        logger.debug("Nothing to purge.", e)
                    }
                }
            }
        })

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
