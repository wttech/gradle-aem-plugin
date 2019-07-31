package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    fun disableOn(workflowName: String, callback: () -> Unit) {
        try {
            disable(workflowName)
            callback()
        } finally {
            enable(workflowName)
        }
    }

    fun enable(workflowName: String) = println("Workflow $workflowName is enabled.")

    fun enable(vararg workflowNames: String) = workflowNames.forEach { enable(it) }

    fun disable(workflowName: String) = println("Workflow $workflowName is disabled.")

    fun disable(vararg workflowNames: String) = workflowNames.forEach { disable(it) }
}