package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.common.CommonException
import java.util.*

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    var toggleRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.workflowManager.toggleRetry") ?: 3) }

    var restoreRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.workflowManager.restoreRetry") ?: 6) }

    var restoreIntended = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.workflowManager.restoreIntended")?.let { set(it) }
    }

    fun launcher(id: String) = WorkflowLauncher(this, id)

    fun launcher(id: String, options: WorkflowLauncher.() -> Unit) = WorkflowLauncher(this, id).run(options)

    fun launchers(type: String) = launchers(listOf(type))

    fun launchers(types: Iterable<String>) = WorkflowType.ids(types).map { WorkflowLauncher(this, it) }

    fun launchers(vararg types: String) = launchers(types.asIterable())

    fun model(id: String) = WorkflowModel(this, id)

    fun payloads(path: String, type: String): Sequence<Node> = instance.sync.repository {
        query {
            path(path)
            type(type)
        }.nodeSequence()
    }.also { nodes ->
        if (nodes.count() == 0) {
            logger.info("No workflow payloads found of type '$type' under path '$path' on $instance")
        } else {
            var message = "Workflow payloads of type '$type' under path '$path' on $instance\":\n" +
                nodes.map { it.name }.take(PAYLOAD_DISPLAY_LIMIT).joinToString("\n")

            if (nodes.count() > PAYLOAD_DISPLAY_LIMIT) {
                message += "\nand ${nodes.count() - PAYLOAD_DISPLAY_LIMIT} more"
            }
            logger.info(message)
        }
    }

    // ----- DSL shorthands -----

    fun toggle(typeFlags: Map<String, Boolean>) {
        typeFlags.forEach { (type, flag) -> toggle(type, flag) }
    }

    fun toggle(type: String, flag: Boolean) = toggle(listOf(type), flag)

    fun toggle(types: Iterable<String>, flag: Boolean) {
        launchers(types).forEach { workflow ->
            if (workflow.exists) {
                workflow.toggle(flag)
            }
        }
    }

    fun toggle(vararg types: String, flag: Boolean) = toggle(types.asIterable(), flag)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(type: String, flag: Boolean, action: () -> Unit) =
        toggleTemporarily(mapOf(type to flag), action)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(typeFlags: Map<String, Boolean>, action: () -> Unit) {
        if (typeFlags.isEmpty()) {
            action()
            return
        }

        val workflows = typeFlags.flatMap { (type, flag) ->
            launchers(type).filter { it.exists }.onEach { it.toggleIntended = flag }
        }
        try {
            toggle(workflows)
            action()
        } finally {
            restore(workflows)
        }
    }

    private fun toggle(workflows: List<WorkflowLauncher>) {
        val stack = Stack<WorkflowLauncher>().apply { addAll(workflows) }
        toggleRetry.withCountdown<Unit, CommonException>("workflow toggle on '${instance.name}'") { no ->
            if (no > 1) {
                aem.logger.info(
                    "Retrying to toggle workflow launchers (${stack.size}) on $instance:\n" +
                        stack.joinToString("\n") { it.node.path }
                )
            }

            while (stack.isNotEmpty()) {
                val current = stack.peek()
                current.toggle()
                stack.pop()
            }
        }
    }

    private fun restore(workflows: List<WorkflowLauncher>) {
        val stack = Stack<WorkflowLauncher>().apply { addAll(workflows) }
        restoreRetry.withCountdown<Unit, CommonException>("workflow restore on '${instance.name}'") { no ->
            if (no > 1) {
                aem.logger.info(
                    "Retrying to restore workflow launchers (${stack.size}) on $instance:\n" +
                        stack.joinToString("\n") { it.node.path }
                )
            }

            while (stack.isNotEmpty()) {
                val current = stack.peek()
                current.restore()
                stack.pop()
            }
        }
    }

    companion object {
        const val PAYLOAD_DISPLAY_LIMIT = 5
    }
}
