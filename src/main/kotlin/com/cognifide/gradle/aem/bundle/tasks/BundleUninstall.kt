package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.check
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : BundleTask() {

    @TaskAction
    fun uninstall() {
        instances.get().check()
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
