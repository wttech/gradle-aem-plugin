package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageDeploy : PackageTask() {

    @TaskAction
    open fun deploy() {
        sync { awaitIf { packageManager.deploy(it) } }
        common.notifier.notify("Package deployed", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
        instances.convention(sling.obj.provider { sling.instances })
        sling.prop.boolean("package.deploy.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
