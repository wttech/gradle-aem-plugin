package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.Formats

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    fun disableWhile(names: List<String>, callback: () -> Unit) {
        try {
            disable(names)
            callback()
        } finally {
            enable(names)
        }
    }

    fun disableWhile(workflow: Workflow, callback: () -> Unit) = disableWhile(workflow.ids, callback)

    fun enable(name: String) {
        find(name)?.saveProperty("enabled", true)
                ?: throw InstanceException("Workflow $name was not found on $instance.")
    }

    fun enable(names: List<String>) = names.forEach { enable(it) }

    fun enable(workflow: Workflow) = enable(workflow.ids)

    fun disable(name: String) {
        find(name)?.saveProperty("enabled", false)
                ?: throw InstanceException("Workflow $name was not found on $instance.")
    }

    fun disable(names: List<String>) = names.forEach { disable(it) }

    fun disable(workflow: Workflow) = disable(workflow.ids)

    private fun find(name: String): Node? {
        val node = repository.node(getLauncherPath(name, instance.version))
        return if (Formats.versionAtLeast(instance.version, "6.4.0"))  {
                if (repository.node(PATH_6_4_LIBS + name).exists) node else null
            } else {
                if(node.exists) node else null
        }
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