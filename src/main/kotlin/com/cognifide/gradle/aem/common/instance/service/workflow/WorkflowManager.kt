package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
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

    fun launchers(types: Iterable<String>) = WorkflowLauncherType.ids(types).map { WorkflowLauncher(this, it) }

    fun launchers(vararg types: String) = launchers(types.asIterable())

    fun model(id: String) = WorkflowModel(this, id)

    // ----- DSL shorthands -----

    /**
     * Schedule workflow for resource at given path.
     */
    fun schedule(modelId: String, resourcePath: String) = model(modelId).schedule(resourcePath)

    /**
     * Schedule workflows for resources under given path.
     */
    fun schedule(modelId: String, resourceRoot: String, resourceType: String) = model(modelId).schedule(resourceRoot, resourceType)

    fun toggle(launcherTypeFlags: Map<String, Boolean>) {
        launcherTypeFlags.forEach { (type, flag) -> toggle(type, flag) }
    }

    fun toggle(launcherType: String, flag: Boolean) = toggle(listOf(launcherType), flag)

    fun toggle(launcherTypes: Iterable<String>, flag: Boolean) {
        launchers(launcherTypes).forEach { workflow ->
            if (workflow.exists) {
                workflow.toggle(flag)
            }
        }
    }

    fun toggle(vararg launcherTypes: String, flag: Boolean) = toggle(launcherTypes.asIterable(), flag)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(launcherType: String, flag: Boolean, action: () -> Unit) =
        toggleTemporarily(mapOf(launcherType to flag), action)

    /**
     * Temporarily enable or disable workflows, do action, then restore workflows to initial state.
     */
    fun toggleTemporarily(launcherTypeFlags: Map<String, Boolean>, action: () -> Unit) {
        if (launcherTypeFlags.isEmpty()) {
            action()
            return
        }

        val workflows = launcherTypeFlags.flatMap { (type, flag) ->
            launchers(type).filter { it.exists }.onEach { it.toggleIntended = flag }
        }
        try {
            toggle(workflows)
            action()
        } finally {
            restore(workflows)
        }
    }

    private fun toggle(launchers: List<WorkflowLauncher>) {
        val stack = Stack<WorkflowLauncher>().apply { addAll(launchers) }
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
}
