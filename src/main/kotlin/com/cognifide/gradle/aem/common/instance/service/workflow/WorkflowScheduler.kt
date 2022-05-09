package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import org.apache.http.HttpStatus

class WorkflowScheduler(val workflow: Workflow) {

    private val instance = workflow.instance
    private val logger = workflow.logger

    fun execute(nodes: Sequence<Node>): Int {
        nodes.forEach { scheduleForNode(it) }
        return nodes.count()
    }

    fun execute(path: String, type: ResourceType): Int {
        val nodes = getNodes(path, type)
        return execute(nodes)
    }

    fun scheduleForNode(node: Node) {

        instance.sync.http {
            val params = mapOf(
                "payload" to node.path,
                "model" to workflow.model.path,
                "payloadType" to "JCR_PATH"
            )

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

    fun getNodes(path: String, type: ResourceType): Sequence<Node> {
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
