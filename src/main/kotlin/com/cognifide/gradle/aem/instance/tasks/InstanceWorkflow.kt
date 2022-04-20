package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowException
import com.cognifide.gradle.aem.common.tasks.Instance
import org.apache.http.HttpStatus
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceWorkflow : Instance() {

    private val notifier = common.notifier
    private var workflowsCount = 0L

    @Internal
    val workflowModel = common.prop.string("instance.workflow") ?: "var/workflow/models/dam/update_asset"

    @Internal
    val workflowPath = common.prop.string("instance.workflow.path") ?: Node.DAM_PATH

    @Internal
    val resourceType = common.prop.string("instance.workflow.resourceType")?.let { ResourceType.valueOf(it) }
        ?: ResourceType.ASSET

    @TaskAction
    fun run() {
        common.progress(anyInstances.size) {
            step = "Examine instances"
            instanceManager.examine(anyInstances)

            logger.lifecycle("Instances to execute: ${anyInstances.names}, resource type: '${resourceType.value}', resources path: '$workflowPath'\n")

            step = "Scheduling workflows"
            anyInstances.forEach {
                val nodes = getResources(it)
                scheduleWorkflows(it, nodes)
                increment()
            }
        }
        notifier.notify(
            "All workflows schedled properly!",
            "Instances: '${anyInstances.names}', path: '$workflowPath', resources type: '${resourceType.value}', workflows scheduled: '$workflowsCount'"
        )
    }

    private fun scheduleWorkflows(instance: com.cognifide.gradle.aem.common.instance.Instance, nodes: Sequence<Node>) {
        val params = mutableMapOf("model" to "$workflowModel", "payloadType" to "JCR_PATH")
        logger.lifecycle(
            "Instance: ${instance.id}\nResources to be processed:\n${nodes.map { node -> node.path }.joinToString("\n")}"
        )
        instance.sync.http {
            nodes.forEach {
                params.put("payload", it.path)
                post(WORKFLOW_POST_URI, params) {
                    if (!it.statusLine.statusCode.equals(HttpStatus.SC_CREATED)) {
                        throw WorkflowException(
                            "Workflow creating failed for ${params.get("payload")} and workflow model: $workflowModel" +
                                "\nStatus: ${it.statusLine}!"
                        )
                    }
                    workflowsCount++
                }
            }
        }
    }
    private fun getResources(instance: com.cognifide.gradle.aem.common.instance.Instance): Sequence<Node> {
        var nodes = sequenceOf<Node>()
        instance.sync.repository {
            nodes = sync.repository.query {
                workflowPath
                type(resourceType.value)
            }.nodeSequence()
        }
        return when (nodes.count()) {
            0 -> throw WorkflowException("There are no resources of type: ${resourceType.value} to be processed in workflow $workflowModel at $workflowPath!")
            else -> nodes
        }
    }

    init {
        description = "Executes given workflow on resources under the specified path."
    }

    companion object {
        const val NAME = "instanceWorkflow"

        const val WORKFLOW_POST_URI = "/etc/workflow/instances.json"
    }
}
