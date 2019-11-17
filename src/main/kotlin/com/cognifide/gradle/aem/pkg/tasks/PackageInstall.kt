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

        aem.progress(instances.size * packages.size) {
            aem.syncFiles(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.get(file)
                    packageManager.install(pkg.path)
                }
            }
        }

        aem.notifier.notify("Package installed", "${packages.fileNames} from ${instances.names}")
    }

    init {
        description = "Installs AEM package on instance(s)."
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
