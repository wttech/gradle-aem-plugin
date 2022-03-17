package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.TaskAction

open class InstanceTail : Instance() {

    @TaskAction
    fun tail() {
        logger.lifecycle("Tailing logs from:\n${anyInstances.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}'" }}")
        logger.lifecycle("Filter incidents using file: ${instanceManager.tailer.incidentFilter.get()}")

        instanceManager.tailer.tail(anyInstances)
    }

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

    companion object {
        const val NAME = "instanceTail"
    }
}
