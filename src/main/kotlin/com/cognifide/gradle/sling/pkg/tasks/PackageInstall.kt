package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageInstall : PackageTask() {

    @TaskAction
    fun install() {
        sync { packageManager.install(it) }
        common.notifier.notify("Package installed", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Installs Sling package on instance(s)."
        sling.prop.boolean("package.install.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
