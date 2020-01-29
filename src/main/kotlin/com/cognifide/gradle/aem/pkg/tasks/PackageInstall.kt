package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageInstall : PackageTask() {

    @TaskAction
    fun install() {
        instances.checkAvailable()
        sync { packageManager.install(it) }
        common.notifier.notify("Package installed", "${packages.fileNames} from ${instances.names}")
    }

    init {
        description = "Installs AEM package on instance(s)."
        awaited = aem.prop.boolean("package.install.awaited") ?: true
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
