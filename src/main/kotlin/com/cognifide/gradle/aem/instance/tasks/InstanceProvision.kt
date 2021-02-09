package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.provision.Status
import com.cognifide.gradle.aem.common.tasks.Instance
import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.tasks.TaskAction

open class InstanceProvision : Instance() {

    private val provisioner by lazy {
        val urls = aem.prop.list("instance.provision.deployPackage.urls")
        when {
            urls != null -> Provisioner(instanceManager).apply { urls.forEach { deployPackage(it) } }
            else -> instanceManager.provisioner
        }
    }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {
        unchanged { enabled.set(false) }
    }

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun provision() {
        instanceManager.examinePrerequisites(instances.get())
        instanceManager.awaitUp(instances.get(), awaitUpOptions)

        val allActions = provisioner.provision(instances.get())

        val performedActions = allActions.filter { it.status != Status.SKIPPED }
        val skippedActions = allActions - performedActions
        val instances = performedActions.map { it.step.instance }.toSet()
        val performed = performedActions.count()
        val ended = performedActions.count { it.status == Status.ENDED }
        val failed = performedActions.count { it.status == Status.FAILED }

        if (performed > 0) {
            val succedded = Formats.percentExplained(ended - failed, ended)
            common.notifier.notify("Instances provisioned", listOf(
                    "Steps: $succedded succeeded on ${instances.size} instance(s)",
                    "${skippedActions.size} skipped",
                    "${allActions.size} in total"
            ).joinToString(", "))
        }
    }

    init {
        description = "Configures instances only in concrete circumstances (only once, after some time etc)"
    }

    companion object {
        const val NAME = "instanceProvision"
    }
}
