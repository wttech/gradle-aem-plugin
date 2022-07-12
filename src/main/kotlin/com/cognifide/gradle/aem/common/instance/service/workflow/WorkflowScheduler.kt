package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import org.apache.http.HttpStatus

class WorkflowScheduler(private val workflow: Workflow) {

    private val instance = workflow.instance

    fun schedule(nodes: Iterable<Node>): Int {
        var count = 0
        nodes.forEach {
            schedule(it)
            count++
        }
        return count
    }

    fun schedule(path: String, type: String): Int =
        schedule(workflow.manager.findPayloadResources(path, type).asIterable())

    fun schedule(node: Node) {

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

    companion object {
        const val INSTANCE_URI = "/etc/workflow/instances.json"
    }
}
