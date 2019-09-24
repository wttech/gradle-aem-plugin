package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : BundleTask() {

    init {
        description = "Uninstalls OSGi bundle on instance(s)."
    }

    @TaskAction
    fun uninstall() {
        instances.checkAvailable()

        aem.progress(instances.size * bundles.size) {
            aem.syncFiles(instances, bundles) { file ->
                increment("${file.name} -> ${instance.name}") {
                    osgiFramework.uninstallBundle(file)
                }
            }
        }

        aem.notifier.notify("Bundle uninstalled", "${bundles.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "bundleUninstall"
    }
}
