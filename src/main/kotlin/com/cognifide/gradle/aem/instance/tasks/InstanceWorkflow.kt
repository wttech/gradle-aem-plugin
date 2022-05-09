package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
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
        common.prop.string(("instance.workflow.dam_asset"))?.let {
            if (it.isNotEmpty()) set(it)
        }
        common.prop.string("instance.workflow.path")?.let { set(it) }
    }

    @Internal
    val resourceType = aem.obj.typed<ResourceType>() {
        convention(ResourceType.ASSET)
        common.prop.string("instance.workflow.resourceType")?.let { set(ResourceType.valueOf(it)) }
    }

    @TaskAction
    fun run() {
        instanceManager.examine(anyInstances)

        logger.lifecycle("Workflow details:\nmodel: '${model.get()}', resourceType: '${resourceType.get().value}', resources path: '${path.get()}'\n")

        common.progress {
            step = "Fetching nodes"
            val nodes = mutableMapOf<com.cognifide.gradle.aem.common.instance.Instance, Sequence<Node>>()

            aem.sync {
                val resources = WorkflowScheduler(workflowManager.workflow(model.get())).getNodes(path.get(), resourceType.get())
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
                "Instances: '$anyInstances', model: ${model.get()}, path: '${path.get()}', resources type: '${resourceType.get()}'"
            )
        }
    }

    init {
        description = "Executes given workflow on resources under the specified path."
    }

    companion object {
        const val NAME = "instanceWorkflow"
    }
}
