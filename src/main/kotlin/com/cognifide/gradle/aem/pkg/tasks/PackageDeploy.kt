package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageDeploy : PackageTask() {

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    val distributed = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("package.deploy.distributed")?.let { set(it) }
    }

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    val uploadForce = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.deploy.uploadForce")?.let { set(it) }
    }

    /**
     * Repeat upload when failed (brute-forcing).
     */
    @Internal
    var uploadRetry = common.retry { afterSquaredSecond(aem.prop.long("package.deploy.uploadRetry") ?: 3) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    var installRetry = common.retry { afterSquaredSecond(aem.prop.long("package.deploy.installRetry") ?: 2) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    val installRecursive = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.deploy.installRecursive")?.let { set(it) }
    }

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    @Input
    val workflowToggle = aem.obj.map<String, Boolean> {
        convention(mapOf())
        aem.prop.map("package.deploy.workflowToggle")?.let { m -> set(m.mapValues { it.value.toBoolean() }) }
    }

    @TaskAction
    open fun deploy() {
        instances.get().checkAvailable()

        sync { file ->
            workflowManager.toggleTemporarily(workflowToggle.get()) {
                packageManager.deploy(file, uploadForce.get(), uploadRetry, installRecursive.get(), installRetry, distributed.get())
            }
        }

        common.notifier.notify("Package deployed", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."

        instances.convention(aem.obj.provider {
            if (distributed.get()) {
                aem.authorInstances
            } else {
                aem.instances
            }
        })

        aem.prop.boolean("package.deploy.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
