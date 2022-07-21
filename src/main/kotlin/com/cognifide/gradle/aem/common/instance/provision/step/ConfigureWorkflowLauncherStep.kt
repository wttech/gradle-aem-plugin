package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowLauncher

class ConfigureWorkflowLauncherStep(provisioner: Provisioner, val wid: String) : AbstractStep(provisioner) {

    lateinit var workflowAction: WorkflowLauncher.() -> Unit

    fun workflow(action: WorkflowLauncher.() -> Unit) {
        this.workflowAction = action
    }

    override fun validate() {
        if (!::workflowAction.isInitialized) {
            throw ProvisionException("Step '${id.get()}' has no workflow action defined!")
        }
    }

    override fun action(instance: Instance) {
        instance.sync {
            workflowManager.launcher(wid).apply(workflowAction)
        }
    }

    init {
        id.set("configureWorkflowLauncher/$wid")
        description.convention("Configuring workflow launcher with id '$wid'")
    }
}
