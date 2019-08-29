package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.utils.Formats

class WorkflowManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val configFrozen = Formats.versionAtLeast(instance.version, "6.4.0")

    fun enable(type: String) = toggle(type, true)

    fun enable(type: Iterable<String>) = type.forEach { enable(it) }

    fun enable(vararg types: String) = enable(types.asIterable())

    fun disable(type: String) = toggle(type, false)

    fun disable(types: Iterable<String>) = types.forEach { disable(it) }

    fun disable(vararg types: String) = disable(types.asIterable())

    fun toggle(types: Iterable<String>, flag: Boolean) {
        types.forEach { toggle(it, flag) }
    }

    fun toggle(vararg types: String, flag: Boolean) = toggle(types.asIterable(), flag)

    fun toggle(typeFlags: Map<String, Boolean>) {
        typeFlags.forEach { (type, flag) -> toggle(type, flag) }
    }

    fun toggle(type: String, flag: Boolean) {
        val workflows = WorkflowType.ids(type).map { Workflow(this, it) }

        workflows.forEach { workflow ->
            if (!workflow.exists) {
                throw InstanceException("Workflow '${workflow.id}' cannot be found on $instance!")
            }
        }

        workflows.forEach { workflow ->
            workflow.toggle(flag)
        }
    }

    fun toggleTemporarily(typeFlags: Map<String, Boolean>, callback: () -> Unit) {
        if (typeFlags.isEmpty()) {
            callback()
            return
        }

        try {
            toggle(typeFlags)
            callback()
        } finally {
            toggle(typeFlags.mapValues { !it.value })
        }
    }
}