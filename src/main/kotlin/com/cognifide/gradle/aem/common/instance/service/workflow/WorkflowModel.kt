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
    fun schedule(resourceRoot: String, resourceType: String) = repository.query {
        path(resourceRoot)
        type(resourceType)
    }.nodeSequence().forEach { schedule(it) }

    fun schedule(path: String) = schedule(repository.node(path))


    fun schedule(node: Node) {
        if (!node.exists) {
            throw WorkflowException("Workflow cannot be scheduled as node does not exist at path '${node.path}'!")
        }

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
                        throw WorkflowException("Workflow scheduling failed for '${node.path}' and model: '$id'\nStatus: ${it.statusLine}!")
                    }
                }
            }
        } catch (e: RepositoryException) {
            throw WorkflowException("Cannot schedule workflow '$id' with payload '${node.path}' on $instance!")
        }
    }

    companion object {

        const val INSTANCES_PATH = "/etc/workflow/instances.json"

        const val ROOT = "/var/workflow/models"
    }
}
