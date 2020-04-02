package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.gradle.api.tasks.TaskAction

@OptIn(ExperimentalCoroutinesApi::class)
open class InstanceTail : InstanceTask() {

    @TaskAction
    fun tail() {
        logger.lifecycle("Tailing logs from instances: ${instances.get().names}")
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
