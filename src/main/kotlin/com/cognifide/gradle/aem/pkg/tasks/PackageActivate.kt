package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.fileNames
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class PackageActivate : PackageTask() {

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.getPackage(file)
                    packageManager.activatePackage(pkg.path)
                }
            }
        }
        aem.notifier.notify("Package activated", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "packageActivate"
    }
}