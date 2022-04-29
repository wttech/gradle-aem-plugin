package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import org.apache.http.HttpStatus

object WorkflowScheduler {

    const val WORKFLOW_POST_URI = "/etc/workflow/instances.json"

    fun execute(workflow: Workflow, path: String, type: String) {

        val nodes = getResources(path, type, workflow.instance)

        workflow.logger.lifecycle(
            "Instance: ${workflow.instance.id}, Resources to be processed:\n${
            nodes.map { node -> node.path }.joinToString("\n")
            }"
        )

        nodes.forEach { scheduleForNode(workflow, it) }
    }

    private fun getResources(path: String, type: String, instance: Instance): Sequence<Node> {

        var nodes = sequenceOf<Node>()

        instance.sync.repository {
            nodes = query {
                path(path)
                type(type)
            }.nodeSequence()
        }
        return when (nodes.count()) {
            0 -> throw WorkflowException("No resources found under given path!")
            else -> nodes
        }
    }

    private fun scheduleForNode(workflow: Workflow, node: Node) {

        workflow.instance.sync.http {
            val params =
                mutableMapOf("payload" to node.path, "model" to workflow.model.path, "payloadType" to "JCR_PATH")
            post(WORKFLOW_POST_URI, params) {
                if (it.statusLine.statusCode != HttpStatus.SC_CREATED) {
                    throw WorkflowException(
                        "Workflow creating failed for ${params.get("payload")} and workflow model: ${workflow.model.name}" +
                            "\nStatus: ${it.statusLine}!"
                    )
                }
            }
        }
    }
}
