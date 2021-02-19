package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.service.workflow.Workflow

class ConfigureWorkflowStep(provisioner: Provisioner, val wid: String) : AbstractStep(provisioner) {

    lateinit var workflowAction: Workflow.() -> Unit

    fun workflow(action: Workflow.() -> Unit) {
        this.workflowAction = action
    }

    override fun validate() {
        if (!::workflowAction.isInitialized) {
            throw ProvisionException("Step '${id.get()}' has no workflow action defined!")
        }
    }

    override fun action(instance: Instance) {
        instance.sync {
            workflowManager.workflow(wid).apply(workflowAction)
        }
    }

    init {
        id.set("configureWorkflow/$wid")
        description.convention("Configuring workflow with id '$wid'")
    }
}
