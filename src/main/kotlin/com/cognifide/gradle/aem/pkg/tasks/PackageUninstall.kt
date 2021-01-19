package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUninstall : Package() {

    @TaskAction
    fun uninstall() {
        sync.action { packageManager.uninstall(it) }
        common.notifier.notify("Package uninstalled", "${files.fileNames} from ${instances.names}")
    }

    init {
        description = "Uninstalls AEM package on instance(s)."
        aem.prop.boolean("package.uninstall.awaited")?.let { sync.awaited.set(it) }
        checkForce()
    }

    companion object {
        const val NAME = "packageUninstall"
    }
}
