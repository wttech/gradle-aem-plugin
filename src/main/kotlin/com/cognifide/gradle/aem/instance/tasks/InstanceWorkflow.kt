package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowException
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowScheduler
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import com.cognifide.gradle.aem.common.tasks.Instance as InstanceTask

open class InstanceWorkflow : InstanceTask() {

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
    val resourceType = aem.obj.string {
        convention(ResourceType.ASSET.value)
        common.prop.string("instance.workflow.resourceType")?.let { set(it) }
    }

    @TaskAction
    fun run() {
        instanceManager.examine(anyInstances)

        if (!model.isPresent) throw WorkflowException("Workflow model is not specified, please specify it via 'instance.workflow' property")

        logger.lifecycle("Workflow details:\nmodel: '${model.get()}', resourceType: '${resourceType.get()}', resources path: '${path.get()}'\n")

        common.progress {
            step = "Fetching nodes"
            val nodes = mutableMapOf<Instance, Sequence<Node>>()

            aem.sync(anyInstances) {
                val resources =
                    WorkflowScheduler(workflowManager.workflow(model.get())).queryNodes(path.get(), resourceType.get())
                nodes[this.instance] = resources
            }

            total = nodes.values.sumOf { it.count() }.toLong()
            step = "Scheduling workflows"

            aem.sync(anyInstances) {
                nodes[this.instance]?.forEach {
                    WorkflowScheduler(workflowManager.workflow(model.get())).scheduleForNode(it)
                    increment()
                }
            }

            notifier.notify(
                "$total workflows scheduled properly!",
                "Instances: '${anyInstances.names}', model: '${model.get()}', path: '${path.get()}', resources type: '${resourceType.get()}'"
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
