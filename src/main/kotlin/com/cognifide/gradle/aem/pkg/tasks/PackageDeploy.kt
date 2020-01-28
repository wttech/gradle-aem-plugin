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
    var distributed: Boolean = aem.prop.flag("package.deploy.distributed")

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = aem.prop.boolean("package.deploy.uploadForce") ?: true

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
    var installRecursive: Boolean = aem.prop.boolean("package.deploy.installRecursive") ?: true

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    @Input
    var workflowToggle: Map<String, Boolean> = aem.prop.map("package.deploy.workflowToggle")
            ?.mapValues { it.value.toBoolean() } ?: mapOf()

    @TaskAction
    open fun deploy() {
        instances.checkAvailable()

        sync { file ->
            workflowManager.toggleTemporarily(workflowToggle) {
                packageManager.deploy(file, uploadForce, uploadRetry, installRecursive, installRetry, distributed)
            }
        }

        common.notifier.notify("Package deployed", "${packages.fileNames} on ${instances.names}")
    }

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = if (distributed) {
                aem.authorInstances
            } else {
                aem.instances
            }
        }

        if (packages.isEmpty()) {
            packages = aem.dependentPackages(this)
        }
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
        awaited = aem.prop.boolean("package.deploy.awaited") ?: true
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
