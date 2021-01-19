package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageInstall : Package() {

    @TaskAction
    fun install() {
        sync.action { packageManager.install(it) }
        common.notifier.notify("Package installed", "${files.fileNames} from ${instances.names}")
    }

    init {
        description = "Installs AEM package on instance(s)."
        aem.prop.boolean("package.install.awaited")?.let { sync.awaited.set(it) }
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
