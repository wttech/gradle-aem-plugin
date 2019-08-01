package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    fun disableWhile(workflowNames: List<String>, callback: () -> Unit) {
        try {
            disable(workflowNames)
            callback()
        } finally {
            enable(workflowNames)
        }
    }

    fun enable(workflow: Workflow) {
        val path = find(workflow.workflowName)
        aem.logger.info("Workflow $path is enabled on $instance.")
    }

    fun enable(workflowName: String) {
        find(workflowName)?.saveProperty("enabled", false)
                ?: throw InstanceException("Workflow $workflowName was not found on $instance.")
    }

    fun enable(workflowNames: List<String>) = workflowNames.forEach { enable(it) }

    fun disable(workflow: Workflow) {
        val path = find(instance.version)
        aem.logger.info("Workflow $path is disabled on $instance.")
    }

    fun disable(workflowName: String) {
        find(workflowName)?.saveProperty("enabled", false)
                ?: throw InstanceException("Workflow $workflowName was not found on $instance.")
    }

    private fun find(workflowName: String): Node? {
        val launcherPath = VersionUtil.getLauncherPath(workflowName, instance.version)
        return repository.node(launcherPath)
    }

    fun disable(workflowNames: List<String>) = workflowNames.forEach { disable(it) }
}