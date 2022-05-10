package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowException
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowScheduler
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceWorkflow : Instance() {

    private val notifier = common.notifier

    @Internal
    val model = aem.obj.string {
        common.prop.string("instance.workflow")?.let { set(it) }
    }

    @Internal
    val path = aem.obj.string {
        convention(Node.DAM_PATH)
        common.prop.string("instance.workflow.path")?.let { set(it) }
    }

    @Internal
    val resourceType = aem.obj.typed<ResourceType>() {
        convention(ResourceType.ASSET)
        common.prop.string("instance.workflow.resourceType")?.let {
            set(
                ResourceType.of(it)
                    ?: throw WorkflowException("Invalid resourceType declared via property 'instance.workflow.resourceType'")
            )
        }
    }

    @TaskAction
    fun run() {
        instanceManager.examine(anyInstances)

        if (!model.isPresent) throw WorkflowException("Workflow model is not specified, please specify it via 'instance.workflow' property")

        logger.lifecycle("Workflow details:\nmodel: '${model.get()}', resourceType: '${resourceType.get().value}', resources path: '${path.get()}'\n")

        common.progress {
            step = "Fetching nodes"
            val nodes = mutableMapOf<com.cognifide.gradle.aem.common.instance.Instance, Sequence<Node>>()

            aem.sync {
                val resources =
                    WorkflowScheduler(workflowManager.workflow(model.get())).queryNodes(path.get(), resourceType.get())
                nodes[this.instance] = resources
            }

            total = nodes.values.map { it.count() }.sum().toLong()
            step = "Scheduling workflows"

            aem.sync {
                nodes[this.instance]?.forEach {
                    WorkflowScheduler(workflowManager.workflow(model.get())).scheduleForNode(it)
                    increment()
                }
            }

            notifier.notify(
                "$total workflows scheduled properly!",
                "Instances: '${anyInstances.names}', model: '${model.get()}', path: '${path.get()}', resources type: '${resourceType.get().value}'"
            )
        }
    }

    init {
        description = "Schedules given workflow on resources under the specified path (or default: ${Node.DAM_PATH})."
    }

    companion object {
        const val NAME = "instanceWorkflow"
    }
}
