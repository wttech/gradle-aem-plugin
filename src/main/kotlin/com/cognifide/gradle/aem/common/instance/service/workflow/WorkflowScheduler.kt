package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import org.apache.http.HttpStatus

class WorkflowScheduler(val workflow: Workflow) {

    private val instance = workflow.instance
    private val logger = workflow.logger

    fun schedule(nodes: Sequence<Node>): Int {
        nodes.forEach { scheduleForNode(it) }
        return nodes.count()
    }

    fun schedule(path: String, type: ResourceType): Int {
        val nodes = queryNodes(path, type)
        return schedule(nodes)
    }

    fun scheduleForNode(node: Node) {

        val params = mapOf(
            "payload" to node.path,
            "model" to workflow.model.path,
            "payloadType" to "JCR_PATH"
        )

        instance.sync.http {
            post(INSTANCE_URI, params) {
                if (it.statusLine.statusCode != HttpStatus.SC_CREATED) {
                    throw WorkflowException(
                        "Workflow scheduling failed for ${params.get("payload")} and workflow model: $workflow" +
                            "\nStatus: ${it.statusLine}!"
                    )
                }
            }
        }
    }

    fun queryNodes(path: String, type: ResourceType): Sequence<Node> {
        return instance.sync.repository {
            query {
                path(path)
                type(type)
            }.nodeSequence()
        }.also {
            var message =
                "Resources found:\n${it.map { it.name }.take(RESOURCES_DISPLAYED_LIMIT).joinToString("\n")}"

            if (it.count() > RESOURCES_DISPLAYED_LIMIT) message += " and ${it.count() - RESOURCES_DISPLAYED_LIMIT} more...\n"

            if (it.count() == 0) message = "No resources found "

            logger.lifecycle("${message}on $instance")
        }
    }

    companion object {
        const val RESOURCES_DISPLAYED_LIMIT = 5
        const val INSTANCE_URI = "/etc/workflow/instances.json"
    }
}
