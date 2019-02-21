package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.fileNames
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class Upload : PackageTask() {

    init {
        description = "Uploads AEM package to instance(s)."
    }

    @TaskAction
    fun upload() {
        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    uploadPackage(pkg)
                }
            }
        }

        aem.notifier.notify("Package uploaded", "${packages.fileNames} from ${instances.names}")
    }

    companion object {
        const val NAME = "aemUpload"
    }
}
