package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : BundleTask() {

    @TaskAction
    fun uninstall() {
        instances.checkAvailable()

        common.progress(instances.size * bundles.size) {
            aem.syncFiles(instances, bundles) { file ->
                increment("${file.name} -> ${instance.name}") {
                    osgiFramework.uninstallBundle(file)
                }
            }
        }

        common.notifier.notify("Bundle uninstalled", "${bundles.fileNames} on ${instances.names}")
    }

    init {
        description = "Uninstalls OSGi bundle on instance(s)."
    }

    companion object {
        const val NAME = "bundleUninstall"
    }
}
