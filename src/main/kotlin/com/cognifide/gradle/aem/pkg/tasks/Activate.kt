package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.TaskAction

open class Activate : Sync() {

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        aem.progress({
            header = "Activating package(s) on instance(s)"
            total = instances.size.toLong() * packages.size.toLong()
        }, {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    activatePackage(determineRemotePackagePath(pkg))
                }
            }
        })

        aem.notifier.notify("Package activated", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemActivate"
    }
}