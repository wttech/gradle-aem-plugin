package com.cognifide.gradle.sling.common.instance.service.workflow

import com.cognifide.gradle.sling.common.instance.InstanceService
import com.cognifide.gradle.sling.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import java.util.*

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    var toggleRetry = common.retry { afterSquaredSecond(sling.prop.long("instance.workflowManager.toggleRetry") ?: 3) }

    var restoreRetry = common.retry { afterSquaredSecond(sling.prop.long("instance.workflowManager.restoreRetry") ?: 6) }

    var restoreIntended = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("instance.workflowManager.restoreIntended")?.let { set(it) }
    }

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
            }
        }
    }

    fun toggle(vararg types: String, flag: Boolean) = toggle(types.asIterable(), flag)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(type: String, flag: Boolean, action: () -> Unit) = toggleTemporarily(mapOf(type to flag), action)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(typeFlags: Map<String, Boolean>, action: () -> Unit) {
        if (typeFlags.isEmpty()) {
            action()
            return
        }

        val workflows = typeFlags.flatMap { (type, flag) ->
            workflows(type).filter { it.exists }.onEach { it.toggleIntended = flag }
        }
        try {
            toggle(workflows)
            action()
        } finally {
            restore(workflows)
        }
    }

    private fun toggle(workflows: List<Workflow>) {
        val stack = Stack<Workflow>().apply { addAll(workflows) }
        toggleRetry.withCountdown<Unit, CommonException>("workflow toggle on '${instance.name}'") { no ->
            if (no > 1) {
                sling.logger.info("Retrying to toggle workflow launchers (${stack.size}) on $instance:\n" +
                        stack.joinToString("\n") { it.launcher.path })
            }

            while (stack.isNotEmpty()) {
                val current = stack.peek()
                current.toggle()
                stack.pop()
            }
        }
    }

    private fun restore(workflows: List<Workflow>) {
        val stack = Stack<Workflow>().apply { addAll(workflows) }
        restoreRetry.withCountdown<Unit, CommonException>("workflow restore on '${instance.name}'") { no ->
            if (no > 1) {
                sling.logger.info("Retrying to restore workflow launchers (${stack.size}) on $instance:\n" +
                        stack.joinToString("\n") { it.launcher.path })
            }

            while (stack.isNotEmpty()) {
                val current = stack.peek()
                current.restore()
                stack.pop()
            }
        }
    }
}
