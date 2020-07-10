package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class PackageDeploy : PackageTask() {

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    val distributed = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("package.deploy.distributed")?.let { set(it) }
    }

    @TaskAction
    open fun deploy() {
        sync { awaitIf { packageManager.deploy(it, distributed.get()) } }
        common.notifier.notify("Package deployed", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."

        instances.convention(sling.obj.provider {
            if (distributed.get()) {
                sling.authorInstances
            } else {
                sling.instances
            }
        })

        sling.prop.boolean("package.deploy.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
