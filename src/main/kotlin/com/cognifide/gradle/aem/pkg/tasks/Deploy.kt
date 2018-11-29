package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Deploy : Sync() {

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
    }

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var distributed: Boolean = aem.props.flag("aem.deploy.distributed")

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = aem.props.boolean("aem.deploy.uploadForce") ?: true

    /**
     * Repeat upload when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var uploadRetry = aem.retry { afterSquaredSecond(aem.props.long("aem.deploy.uploadRetry") ?: 6) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var installRetry = aem.retry { afterSquaredSecond(aem.props.long("aem.deploy.installRetry") ?: 4) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var installRecursive: Boolean = aem.props.boolean("aem.deploy.installRecursive") ?: true

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = if (distributed) {
                aem.instanceAuthors
            } else {
                aem.instances
            }
        }

        if (packages.isEmpty()) {
            packages = aem.packagesDependent(this)
        }
    }

    @TaskAction
    open fun deploy() {
        // TODO sync interactively: \ Deploying packages on instances (50%): example.full.zip on local-author, local-publish
        aem.syncPackages(instances, packages) { pkg ->
            if (distributed) {
                distributePackage(pkg)
            } else {
                deployPackage(pkg)
            }
        }

        aem.notifier.notify("Package deployed", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemDeploy"
    }
}