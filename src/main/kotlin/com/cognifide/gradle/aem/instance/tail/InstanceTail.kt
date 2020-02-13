package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@UseExperimental(ExperimentalCoroutinesApi::class)
open class InstanceTail : InstanceTask() {

    @Internal
    val tailer = InstanceTailer(aem)

    fun tailer(options: InstanceTailer.() -> Unit) {
        tailer.apply(options)
    }

    @TaskAction
    fun tail() {
        tailer.apply {
            instances.convention(this@InstanceTail.instances)

            logger.lifecycle("Tailing logs from instances: ${instances.get().names}")
            logger.lifecycle("Filter incidents using file: ${tailer.incidentFilter.get()}")

            tail()
        }
    }

    override fun projectEvaluated() {
        super.projectEvaluated()
        tailer.logFilter.excludeFile(tailer.incidentFilter.get().asFile)
    }

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

    companion object {
        const val NAME = "instanceTail"
    }
}
