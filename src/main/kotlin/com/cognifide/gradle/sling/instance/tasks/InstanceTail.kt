package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.tasks.InstanceTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.gradle.api.tasks.TaskAction

@OptIn(ExperimentalCoroutinesApi::class)
open class InstanceTail : InstanceTask() {

    @TaskAction
    fun tail() {
        logger.lifecycle("Tailing logs from:\n${instances.get().joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}'" }}")
        logger.lifecycle("Filter incidents using file: ${instanceManager.tailer.incidentFilter.get()}")

        instanceManager.tailer.tail(instances.get())
    }

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

    companion object {
        const val NAME = "instanceTail"
    }
}
