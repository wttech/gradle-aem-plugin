package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.workflow.WorkflowException
import com.cognifide.gradle.aem.common.utils.fileNames
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
    val resourcePath = aem.obj.string {
        common.prop.string("instance.workflow.resourcePath")?.let { set(it) }
    }

    @Internal
    val resourceType = aem.obj.string {
        common.prop.string("instance.workflow.resourceType")?.let { set(it) }
    }

    @TaskAction
    fun doAction() {
        checkInputs()
        scheduleWorkflows()
    }

    private fun checkInputs() {
        if (model.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow model is not defined, specify it via property 'instance.workflow.model'!")
        }
        if (resourcePath.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow resource path is not defined, specify it via property 'instance.workflow.resourcePath'!")
        }
        if (resourceType.orNull.isNullOrBlank()) {
            throw WorkflowException("Workflow resource type is not defined, specify it via property 'instance.workflow.resourceType'!")
        }
    }

    private fun scheduleWorkflows() {
        instanceManager.examine(anyInstances)

        logger.lifecycle(
            listOf(
                "Scheduling workflows with following details:",
                "Workflow model: '${model.get()}'",
                "Resource path: '${resourcePath.get()}'",
                "Resource type: '${resourceType.get()}'",
            ).joinToString("\n")
        )

        common.progress {
            step = "Fetching workflow payloads"
            val nodes = mutableMapOf<Instance, Sequence<Node>>()

            aem.sync(anyInstances) {
                nodes[instance] = workflowManager.payloads(resourcePath.get(), resourceType.get())
            }

            total = nodes.values.sumOf { it.count() }.toLong()
            step = "Scheduling workflows"

            aem.sync(anyInstances) {
                val model = workflowManager.model(model.get())
                nodes[this.instance]?.forEach {
                    model.schedule(it)
                    increment()
                }
            }

            common.notifier.notify("Workflows scheduled", "$total resource(s) on ${anyInstances.names}")
        }
    }

    init {
        description = "Schedules workflow model on resources under the specified path"
    }

    companion object {
        const val NAME = "instanceWorkflow"
    }
}
