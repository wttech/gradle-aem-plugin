package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUninstall : PackageTask() {

    @TaskAction
    fun uninstall() {
        sync { packageManager.uninstall(it) }
        common.notifier.notify("Package uninstalled", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Uninstalls Sling package on instance(s)."
        sling.prop.boolean("package.uninstall.awaited")?.let { awaited.set(it) }
        checkForce()
    }

    companion object {
        const val NAME = "packageUninstall"
    }
}
