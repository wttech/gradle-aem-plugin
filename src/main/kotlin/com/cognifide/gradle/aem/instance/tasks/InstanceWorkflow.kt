package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import com.cognifide.gradle.aem.common.tasks.Instance as InstanceTask

open class InstanceWorkflow : InstanceTask() {

    private val notifier = common.notifier

    @Internal
    val model = aem.obj.string {
        common.prop.string("instance.workflow.model")?.let { set(it) }
    }

    @Internal
    val path = aem.obj.string {
        convention(Node.DAM_PATH)
        common.prop.string("instance.workflow.path")?.let { set(it) }
    }

    @Internal
    val resourceType = aem.obj.string {
        convention(ResourceType.ASSET.value)
        common.prop.string("instance.workflow.resourceType")?.let { set(it) }
    }

    @TaskAction
    fun run() {
        instanceManager.examine(anyInstances)

        if (model.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow model is not defined, specify it via property 'instance.workflow.model'")
        }
        if (path.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow path is not defined, specify it via property 'instance.workflow.path'")
        }
        if (resourceType.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow resource type is not defined, specify it via property 'instance.workflow.resourceType'")
        }

        logger.lifecycle("Workflow details:\nmodel: '${model.get()}', resourceType: '${resourceType.get()}', resources path: '${path.get()}'\n")

        common.progress {
            step = "Fetching nodes"
            val nodes = mutableMapOf<Instance, Sequence<Node>>()

            aem.sync(anyInstances) {
                nodes[this.instance] = workflowManager.queryNodes(path.get(), resourceType.get())
            }

            total = nodes.values.sumOf { it.count() }.toLong()
            step = "Scheduling workflows"

            aem.sync(anyInstances) {
                val workflow = workflowManager.workflow(model.get())
                nodes[this.instance]?.forEach {
                    workflow.schedule(it)
                    increment()
                }
            }

            notifier.notify(
                "Scheduled workflows: $total!",
                "Instances: '${anyInstances.names}', model: '${model.get()}' on '${path.get()}'"
            )
        }
    }

    init {
        description = "Schedules workflow model on resources under the specified path (or default: '${Node.DAM_PATH}')."
    }

    companion object {
        const val NAME = "instanceWorkflow"
    }
}
