package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.repository.ResourceType
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceWorkflow : Instance() {

    private val notifier = common.notifier

    @Internal
    val model = aem.obj.string {
        convention("update_asset")
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

        logger.lifecycle("Workflow details: Model: '${model.get()}', resourceType: '${resourceType.get()}', resources path: '${path.get()}'\n")

        aem.sync {
            workflowManager.workflow(model.get()).schedule(path.get(), resourceType.get())
        }

        notifier.notify(
            "All workflows scheduled properly!",
            "Instances: '${anyInstances.names}', model: ${model.get()}, path: '${path.get()}', resources type: '${resourceType.get()}''"
        )
    }

    init {
        description = "Executes given workflow on resources under the specified path."
    }

    companion object {
        const val NAME = "instanceWorkflow"
    }
}
