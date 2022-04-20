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
    val workflow = common.prop.string("instance.workflow")

    @Internal
    val workflowPath = common.prop.string("instance.workflow.path") ?: Node.DAM_PATH

    @Internal
    val resourceType = common.prop.string("instance.workflow.resourceType")?.let { ResourceType.valueOf(it) }
        ?: ResourceType.ASSET

    @Internal
    val targetInstaces = when {
        common.prop.string("instance.author") != null -> aem.localInstances
        common.prop.string("instance.publish") != null -> aem.authorInstances
        else -> anyInstances
    }

    @TaskAction
    fun run() {
        instanceManager.examine(targetInstaces)

        if (workflow == null) throw WorkflowException("No workflow model defined!")
        logger.lifecycle("Instances to execute: ${targetInstaces.names}, resource type: '${resourceType.value}', resources path: '$workflowPath'\n")
        val params = mutableMapOf("model" to "$workflow", "payloadType" to "JCR_PATH")
        common.progressIndicator {
            targetInstaces.forEach {
                step = "Getting resources"
                val nodes = getResources(it)

                total = nodes.count().toLong().also { workflowsCount += it }

                logger.lifecycle(
                    "Instance: ${it.id}\nResources to be processed:\n${nodes.map { node -> node.path }.joinToString("\n")}"
                )

                step = "Scheduling workflows"

                it.sync.http {
                    nodes.forEach {
                        params.put("payload", it.path)
                        post(WORKFLOW_POST_URI, params) {
                            if (!it.statusLine.statusCode.equals(HttpStatus.SC_CREATED)) {
                                throw WorkflowException(
                                    "Workflow creating failed for ${params.get("payload")} and workflow model: $workflow" +
                                        "\nStatus: ${it.statusLine}!"
                                )
                            }
                        }
                        increment()
                    }
                }
                reset()
            }
        }
        notifier.notify(
            "All workflows schedled properly!",
            "Instances: '${targetInstaces.names}', path: '$workflowPath', resources type: '${resourceType.value}'"
        )
    }

    private fun getResources(instance: com.cognifide.gradle.aem.common.instance.Instance): Sequence<Node> {
        var nodes = sequenceOf<Node>()
        instance.sync.repository {
            nodes = sync.repository.query {
                workflowPath
                type(resourceType.value)
            }.nodeSequence()
        }
        if (nodes.count() == 0) {
            throw WorkflowException("There are no resources of type: ${resourceType.value} to be processed in workflow at $workflowPath!")
        }
        return nodes
    }

    init {
        description = "Executing given workflow on resources under the specified path"
    }

    companion object {
        const val NAME = "instanceWorkflow"

        const val WORKFLOW_POST_URI = "/etc/workflow/instances.json"
    }
}
