package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageActivate : PackageTask() {

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        checkInstances()

        aem.progress(instances.size * packages.size) {
            aem.syncPackages(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    val pkg = packageManager.get(file)
                    packageManager.activate(pkg.path)
                }
            }
        }
        aem.notifier.notify("Package activated", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "packageActivate"
    }
}