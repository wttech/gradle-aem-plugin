package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUpload : PackageTask() {

    @TaskAction
    fun upload() {
        instances.checkAvailable()

        aem.progress(instances.size * packages.size) {
            aem.syncFiles(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    packageManager.upload(pkg)
                }
            }
        }

        aem.notifier.notify("Package uploaded", "${packages.fileNames} from ${instances.names}")
    }

    init {
        description = "Uploads AEM package to instance(s)."
    }

    companion object {
        const val NAME = "packageUpload"
    }
}
