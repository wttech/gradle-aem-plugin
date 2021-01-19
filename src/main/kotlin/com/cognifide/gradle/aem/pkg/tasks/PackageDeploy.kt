package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class PackageDeploy : Package() {

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    val distributed = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("package.deploy.distributed")?.let { set(it) }
    }

    @TaskAction
    open fun deploy() {
        sync.actionAwaited { packageManager.deploy(it, distributed.get()) }
        common.notifier.notify("Package deployed", "${files.fileNames} on ${instances.names}")
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."

        sync.instances.convention(aem.obj.provider {
            if (distributed.get()) {
                aem.authorInstances
            } else {
                aem.instances
            }
        })

        aem.prop.boolean("package.deploy.awaited")?.let { sync.awaited.set(it) }
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
