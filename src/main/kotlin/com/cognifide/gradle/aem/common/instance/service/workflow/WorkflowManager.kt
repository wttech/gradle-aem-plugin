package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException
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
        val workflows = workflows(types)

        workflows.forEach { workflow ->
            if (!workflow.exists) {
                throw InstanceException("Workflow '${workflow.id}' cannot be found on $instance!")
            }
        }

        workflows.forEach { workflow ->
            workflow.toggle(flag)
        }
    }

    fun toggle(vararg types: String, flag: Boolean) = toggle(types.asIterable(), flag)

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