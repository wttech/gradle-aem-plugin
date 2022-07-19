package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.RepositoryException
import org.apache.http.HttpStatus

class WorkflowModel(val manager: WorkflowManager, val id: String) {

    val instance = manager.instance

    private val repository = manager.repository

    private val logger = manager.aem.logger

    val node by lazy {
        repository.node("$ROOT/$id").takeIf { it.exists }
            ?: throw WorkflowException("Workflow model '$id' not found under path '$ROOT!'")
    }

    fun schedule(nodes: Iterable<Node>): Int {
        var count = 0
        nodes.forEach {
            schedule(it)
            count++
        }
        return count
    }

    fun schedule(path: String, resourceType: String): Int =
        schedule(manager.payloads(path, resourceType).asIterable())

    fun schedule(node: Node) {
        try {
            logger.info("Scheduling workflow '$id' with payload '${node.path}' on $instance")
            instance.sync.http {
                val params = mapOf(
                    "payload" to node.path,
                    "model" to this@WorkflowModel.node.path,
                    "payloadType" to "JCR_PATH"
                )
                post(INSTANCES_PATH, params) {
                    if (it.statusLine.statusCode != HttpStatus.SC_CREATED) {
                        throw WorkflowException(
                            "Workflow scheduling failed for '${node.path}' and model: '$id'" +
                                "\nStatus: ${it.statusLine}!"
                        )
                    }
                }
            }
            logger.info("Scheduled workflow '$id' with payload '${node.path}' on $instance")
        } catch (e: RepositoryException) {
            throw WorkflowException("Cannot schedule workflow '$id' with payload '${node.path}' on $instance!")
        }
    }

    companion object {

        const val INSTANCES_PATH = "/etc/workflow/instances.json"

        const val ROOT = "/var/workflow/models"
    }
}
