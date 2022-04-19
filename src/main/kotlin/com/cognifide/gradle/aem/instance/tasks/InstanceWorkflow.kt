package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceWorkflow : Instance() {

    private val notifier = common.notifier

    @Internal
    val workflow = common.prop.string("instance.workflow") ?: error("No workflow defined!")

    @Internal
    val workflowPath = common.prop.string("instance.workflow.path") ?: error("No worflow path defined!")

    @TaskAction
    fun run() = anyInstances.forEach { instanceWorkflows(it) }.let { notifier.notify("All workflows executed correctly!", "Instances: ${anyInstances.names}") }

    fun instanceWorkflows(instance: com.cognifide.gradle.aem.common.instance.Instance) {
        instance.sync.repository {
            val nodes = sync.repository.query {
                path(workflowPath)
                type(NODE_TYPE)
            }.nodeSequence()

            if (nodes.count() == 0) error("No assets avaialable for processing!")

            logger.lifecycle(">>> Instance: ${instance.id}, Assets to be processed:\n${nodes.map { node -> node.path }.joinToString("\n")}")

            instance.sync.http {
                nodes.forEach {
                    val params = mapOf("model" to "$workflow", "payloadType" to "JCR_PATH", "payload" to "${it.path}")
                    post(WORKFLOW_POST_URI, params) {
                        if (!it.statusLine.statusCode.equals(HTTP_SUCCESS_CODE)) error("Workflow creating failed for params: $params!")
                    }
                }
            }
        }
    }

    init {
        description = "Executing given worflow in given path"
    }

    companion object {
        const val NAME = "instanceWorkflow"

        const val WORKFLOW_POST_URI = "/etc/workflow/instances.json"
        const val NODE_TYPE = "dam:Asset"
        const val HTTP_SUCCESS_CODE = 201
    }
}
