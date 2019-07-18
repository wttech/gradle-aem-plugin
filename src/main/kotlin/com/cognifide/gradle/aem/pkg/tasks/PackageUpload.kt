package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUpload : PackageTask() {

    init {
        description = "Uploads AEM package to instance(s)."
    }

    @TaskAction
    fun upload() {
        checkInstances()

        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    packageManager.upload(pkg)
                }
            }
        }

        aem.notifier.notify("Package uploaded", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "packageUpload"
    }
}
