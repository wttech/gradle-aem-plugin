package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import org.apache.http.HttpStatus

class WorkflowScheduler(private val workflow: Workflow) {

    private val instance = workflow.instance

    private val logger = workflow.logger

    fun schedule(nodes: Iterable<Node>): Int {
        nodes.forEach { scheduleForNode(it) }
        return nodes.count()
    }

    fun schedule(path: String, type: String): Int = schedule(queryNodes(path, type).asIterable())

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
                        "Workflow scheduling failed for '${params["payload"]}' and model: '$workflow'" +
                            "\nStatus: ${it.statusLine}!"
                    )
                }
            }
        }
    }

    fun queryNodes(path: String, type: String): Sequence<Node> = instance.sync.repository {
        query {
            path(path)
            type(type)
        }.nodeSequence()
    }.also {
        var message =
            "Resources found:\n${it.map { it.name }.take(RESOURCES_DISPLAY_LIMIT).joinToString("\n")}"

        if (it.count() > RESOURCES_DISPLAY_LIMIT) message += " and ${it.count() - RESOURCES_DISPLAY_LIMIT} more"
        if (it.count() == 0) message = "No resources found"

        logger.lifecycle("$message on $instance")
    }

    companion object {
        const val RESOURCES_DISPLAY_LIMIT = 5
        const val INSTANCE_URI = "/etc/workflow/instances.json"
    }
}
