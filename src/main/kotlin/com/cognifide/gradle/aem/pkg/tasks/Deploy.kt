package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.fileNames
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.instance.names
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Deploy : Sync() {

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
    }

    /**
     * Check instance(s) health after deploying package(s).
     */
    @Input
    var awaited: Boolean = aem.props.boolean("aem.deploy.awaited") ?: true

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

    private var awaitOptions: AwaitAction.() -> Unit = {}

    /**
     * Controls await action.
     */
    fun await(options: AwaitAction.() -> Unit) {
        this.awaitOptions = options
    }

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
        aem.progress({
            header = "Deploying package(s) to instance(s)"
            total = instances.size.toLong() * packages.size.toLong()
        }, {
            aem.syncPackages(instances, packages) { pkg ->
                increment("${pkg.name} -> ${instance.name}") {
                    if (distributed) {
                        distributePackage(pkg, uploadForce, uploadRetry, installRecursive, installRetry)
                    } else {
                        deployPackage(pkg, uploadForce, uploadRetry, installRecursive, installRetry)
                    }
                }
            }
        })

        if (awaited) {
            aem.actions.await {
                instances = this@Deploy.instances
                awaitOptions()
            }
        }

        aem.notifier.notify("Package deployed", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemDeploy"
    }
}