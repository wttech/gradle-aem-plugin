package com.cognifide.gradle.sling.bundle.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.BundleTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : BundleTask() {

    @TaskAction
    fun uninstall() {
        sync { osgiFramework.uninstallBundle(it) }
        common.notifier.notify("Bundle uninstalled", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Uninstalls OSGi bundle on instance(s)."
    }

    companion object {
        const val NAME = "bundleUninstall"
    }
}
