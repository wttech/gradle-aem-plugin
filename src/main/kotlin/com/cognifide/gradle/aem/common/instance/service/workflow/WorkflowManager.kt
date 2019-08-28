package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_1
import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_4
import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_4_LIBS
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.Formats

// TODO introduce Workflow class which will hide launcher impl details
class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val strategy: ToggleStrategy
        get() = when {
            Formats.versionAtLeast(instance.version, "6.4.0") -> Toggle64()
            else -> Toggle61()
        }

    fun toggle(names: List<String>, expectedState: Boolean, callback: () -> Unit) {
        try {
            toggleAll(names, expectedState)
            callback()
        } finally {
            toggleAll(names, !expectedState)
        }
    }

    fun enable(name: String) = saveState(name, true)

    fun enable(names: List<String>) = names.forEach { enable(it) }

    fun disable(name: String) = saveState(name, false)

    fun disable(names: List<String>) = names.forEach { disable(it) }

    private fun find(name: String): Node? {
        val node = repository.node(determineLauncherPath(name, instance.version))
        return when {
            Formats.versionAtLeast(instance.version, "6.4.0") -> if (repository.node("$WF_LAUNCHER_PATH_6_4_LIBS$name").exists) node else null
            else -> if (node.exists) node else null
        }
    }

    private fun toggleAll(names: List<String>, expectedState: Boolean) {
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
        find(name)?.let { strategy.toggle(it, state) }
                ?: throw InstanceException("Workflow $name was not found on $instance.")
    }

    companion object {
        fun determineLauncherPath(name: String, aemVersion: String): String {
            return if (Formats.versionAtLeast(aemVersion, "6.4.0")) "$WF_LAUNCHER_PATH_6_4$name"
            else "$WF_LAUNCHER_PATH_6_1$name"
        }
    }
}