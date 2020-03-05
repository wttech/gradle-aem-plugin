package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageInstall : PackageTask() {

    @TaskAction
    fun install() {
        sync { packageManager.install(it) }
        common.notifier.notify("Package installed", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Installs AEM package on instance(s)."
        aem.prop.boolean("package.install.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
