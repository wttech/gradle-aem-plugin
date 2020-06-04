package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.provision.Status
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceProvision : InstanceTask() {

    private val provisioner by lazy {
        val urls = aem.prop.list("instance.provision.deployPackage.urls")
        when {
            urls != null -> Provisioner(instanceManager).apply { urls.forEach { deployPackage(it) } }
            else -> instanceManager.provisioner
        }
    }

    @TaskAction
    fun provision() {
        instanceManager.examine(instances.get())

        val allActions = provisioner.provision(instances.get())
        val performedActions = allActions.filter { it.status != Status.SKIPPED }
        val instances = performedActions.map { it.step.instance }.toSet()
        val performed = performedActions.count()
        val ended = performedActions.count { it.status == Status.ENDED }
        val failed = performedActions.count { it.status == Status.FAILED }

        if (performed > 0) {
            common.notifier.notify("Instances provisioned", "Performed $performed steps(s)" +
                    " ($ended ended, $failed failed) on ${instances.size} instance(s).")
        }
    }

    init {
        description = "Configures instances only in concrete circumstances (only once, after some time etc)"
    }

    companion object {
        const val NAME = "instanceProvision"
    }
}
