package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.Formats

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    fun toggleWhile(names: List<String>, expectedState: Boolean, callback: () -> Unit) {
        try {
            names.forEach { name ->
                //todo move  "change required' block to Toggle class
                val currentState = find(name)?.properties?.get("enabled")
                val changeRequired = expectedState != currentState
                if (changeRequired) {
                    toggle(name, expectedState)
                    aem.logger.info("Workflow $name switched on $instance. Current state: $expectedState")
                }
            }
            callback()
        } finally {
            names.forEach { name ->
                val currentState = find(name)?.properties?.get("enabled")
                val changeRequired = expectedState == currentState
                if (changeRequired) {
                    toggle(name, !expectedState)
                    aem.logger.info("Workflow $name switched back on $instance. Current state: ${!expectedState}")
                }
            }
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
        val node = repository.node(getLauncherPath(name, instance.version))
        return if (Formats.versionAtLeast(instance.version, "6.4.0")) {
                if (repository.node(PATH_6_4_LIBS + name).exists) node else null
            } else {
                if (node.exists) node else null
        }
    }

    private fun toggle(name: String, enable: Boolean) {
        if (enable) {
            enable(name)
        } else {
            disable(name)
        }
    }

    private fun saveState(name: String, state: Boolean) {
        val strategy: ToggleStrategy =
                if (Formats.versionAtLeast(instance.version, "6.4.0")) Toggle64() else Toggle61()
        find(name)?.let { launcherNode ->
            strategy.toggle(launcherNode, state)
        } ?: throw InstanceException("Workflow $name was not found on $instance.")
    }

    companion object {
        private const val PATH_6_1 = "/etc/workflow/launcher/config/"

        private const val PATH_6_4 = "/conf/global/settings/workflow/launcher/config/"

        private const val PATH_6_4_LIBS = "/libs/settings/workflow/launcher/config/"

        fun getLauncherPath(name: String, aemVersion: String): String {
            return if (Formats.versionAtLeast(aemVersion, "6.4.0")) PATH_6_4 + name
            else PATH_6_1 + name
        }
    }
}