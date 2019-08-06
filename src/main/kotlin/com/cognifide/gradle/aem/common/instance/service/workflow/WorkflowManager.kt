package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_1
import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_4
import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_4_LIBS
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.Formats

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    fun toggleWhile(names: List<String>, expectedState: Boolean, callback: () -> Unit) {
        val strategy: ToggleStrategy =
                if (Formats.versionAtLeast(instance.version, "6.4.0")) Toggle64() else Toggle61()
        try {
            toggleAll(names, expectedState, strategy)
            callback()
        } finally {
            toggleAll(names, !expectedState, strategy)
        }
    }

    fun toggleWhile(workflow: Workflow, expectedState: Boolean, callback: () -> Unit) = toggleWhile(workflow.ids, expectedState, callback)

    fun enable(name: String) = saveState(name, true)

    fun enable(names: List<String>) = names.forEach { enable(it) }

    fun enable(workflow: Workflow) = enable(workflow.ids)

    fun disable(name: String) = saveState(name, false)

    fun disable(names: List<String>) = names.forEach { disable(it) }

    fun disable(workflow: Workflow) = disable(workflow.ids)

    private fun find(name: String): Node? {
        val node = repository.node(determineLauncherPath(name, instance.version))
        return if (Formats.versionAtLeast(instance.version, "6.4.0")) {
                if (repository.node(WF_LAUNCHER_PATH_6_4_LIBS + name).exists) node else null
            } else {
                if (node.exists) node else null
        }
    }

    private fun toggleAll(names: List<String>, expectedState: Boolean, strategy: ToggleStrategy) {
        names.forEach { name ->
            find(name)?.let { launcher ->
                if (strategy.changeRequired(launcher, expectedState)) {
                    strategy.toggle(launcher, expectedState)
                    aem.logger.info("Workflow $name switched to $expectedState on $instance.")
                }
            } ?: throw InstanceException("Workflow $name was not found on $instance.")
        }
    }

    private fun saveState(name: String, state: Boolean) {
        val strategy: ToggleStrategy =
                if (Formats.versionAtLeast(instance.version, "6.4.0")) Toggle64() else Toggle61()
        find(name)?.let { launcher ->
            strategy.toggle(launcher, state)
        } ?: throw InstanceException("Workflow $name was not found on $instance.")
    }

    companion object {
        fun determineLauncherPath(name: String, aemVersion: String): String {
            return if (Formats.versionAtLeast(aemVersion, "6.4.0")) "$WF_LAUNCHER_PATH_6_4$name"
            else "$WF_LAUNCHER_PATH_6_1$name"
        }
    }
}