package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Bundle
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : Bundle() {

    @TaskAction
    fun uninstall() {
        sync.action { osgi.uninstallBundle(it) }
        common.notifier.notify("Bundle uninstalled", "${files.fileNames} on ${instances.names}")
    }

    init {
        description = "Uninstalls OSGi bundle on instance(s)."
    }

    companion object {
        const val NAME = "bundleUninstall"
    }
}
