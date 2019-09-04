package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.utils.Formats

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val configFrozen: Boolean
        get() = Formats.versionAtLeast(instance.version, "6.4.0")

    fun workflow(id: String) = Workflow(this, id)

    fun workflows(type: String) = workflows(listOf(type))

    fun workflows(types: Iterable<String>) = WorkflowType.ids(types).map { Workflow(this, it) }

    fun workflows(vararg types: String) = workflows(types.asIterable())

    // ----- DSL shorthands -----

    fun toggle(typeFlags: Map<String, Boolean>) {
        typeFlags.forEach { (type, flag) -> toggle(type, flag) }
    }

    fun toggle(type: String, flag: Boolean) = toggle(listOf(type), flag)

    fun toggle(types: Iterable<String>, flag: Boolean) {
        workflows(types).forEach { workflow ->
            if (workflow.exists) {
                workflow.toggle(flag)
            } else {
                aem.logger.warn("Workflow '${workflow.id}' does not exist on $instance!")
            }
        }
    }

    fun toggle(vararg types: String, flag: Boolean) = toggle(types.asIterable(), flag)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(typeFlags: Map<String, Boolean>, action: () -> Unit) {
        if (typeFlags.isEmpty()) {
            action()
            return
        }

        val workflowToFlag = typeFlags.map { (type, flag) ->
            workflows(type).filter { workflow ->
                val exists = workflow.exists
                if (!exists) {
                    aem.logger.warn("Workflow '${workflow.id}' does not exist on $instance!")
                }
                exists
            } to (flag)
        }

        try {
            workflowToFlag.forEach { (workflows, flag) -> workflows.forEach { it.toggle(flag) } }
            action()
        } finally {
            workflowToFlag.forEach { (workflows, _) -> workflows.forEach { it.restore() } }
        }
    }
}
