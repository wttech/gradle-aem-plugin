package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageInstall : PackageTask() {

    init {
        description = "Installs AEM package on instance(s)."
    }

    @TaskAction
    fun install() {
        checkInstances()

        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.get(file)
                    packageManager.install(pkg.path)
                }
            }
        }

        aem.notifier.notify("Package installed", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "packageInstall"
    }
}
