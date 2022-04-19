package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.apache.http.HttpStatus
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceWorkflow : Instance() {

    private val notifier = common.notifier
    private var nodesCount = 0

    @Internal
    val workflow = common.prop.string("instance.workflow")

    @Internal
    val workflowPath = common.prop.string("instance.workflow.path")

    @Internal
    val targetInstaces = when {
        common.prop.string("instance.author") != null -> aem.localInstances
        common.prop.string("instance.publish") != null -> aem.authorInstances
        else -> anyInstances
    }

    @TaskAction
    fun run() {
        instanceManager.examine(targetInstaces)
        when {
            workflow == null -> throw InstanceException("No workflow model defined!")
            workflowPath == null -> throw InstanceException("No path defined!")
            else -> {
                targetInstaces.forEach { instanceWorkflows(it) }
                notifier.notify("All workflows schedled properly!", "Instances: ${targetInstaces.names}")
            }
        }
    }

    fun instanceWorkflows(instance: com.cognifide.gradle.aem.common.instance.Instance) {
        instance.sync.repository {
            val nodes = sync.repository.query {
                workflowPath?.let { path(it) }
                type(NODE_TYPE)
            }.nodeSequence()

            if (nodes.count() == 0) throw InstanceException("There are no assets to be processed in workflow at $workflowPath!")
            nodesCount += nodes.count()

            logger.lifecycle("Instance: ${instance.id}, Assets to be processed:\n${nodes.map { node -> node.path }.joinToString("\n")}")

            instance.sync.http {
                common.progress(nodes.count()) {
                    nodes.forEach {
                        val params = mapOf("model" to "$workflow", "payloadType" to "JCR_PATH", "payload" to it.path)
                        post(WORKFLOW_POST_URI, params) {
                            if (!it.statusLine.statusCode.equals(HttpStatus.SC_CREATED)) {
                                throw InstanceException(
                                    "Workflow creating failed for ${params.get("payload")} and workflow model: $workflow" +
                                        "\nStatus: ${it.statusLine}!"
                                )
                            }
                        }
                        increment()
                    }
                }
            }
        }
    }

    init {
        description = "Executing given workflow on assets under the specified path"
    }

    companion object {
        const val NAME = "instanceWorkflow"

        const val WORKFLOW_POST_URI = "/etc/workflow/instances.json"
        const val NODE_TYPE = "dam:Asset"
    }
}
