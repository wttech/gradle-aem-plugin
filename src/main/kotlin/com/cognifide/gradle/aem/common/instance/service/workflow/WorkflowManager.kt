package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.utils.Formats
import java.util.*

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val configFrozen: Boolean
        get() = Formats.versionAtLeast(instance.version, "6.4.0")

    var restoreRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.workflowManager.restoreRetry") ?: 6) }

    var restoreIntended = aem.prop.boolean("instance.workflowManager.restoreIntended") ?: false

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
            workflows.forEach { it.toggle() }
            action()
        } finally {
            val stack = Stack<Workflow>().apply { addAll(workflows) }
            restoreRetry.withCountdown<Unit, AemException>("workflow restore on '${instance.name}'") { no ->
                if (no > 1) {
                    aem.logger.info("Retrying to restore workflow launchers (${stack.size}) on $instance:\n" +
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
}
