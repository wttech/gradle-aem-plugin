package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.tasks.InstanceTask
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@UseExperimental(ObsoleteCoroutinesApi::class)
open class InstanceTail : InstanceTask() {

    @Internal
    val tailer = InstanceTailer(aem)

    fun tailer(options: InstanceTailer.() -> Unit) {
        tailer.apply(options)
    }

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

    @TaskAction
    fun tail() {
        tailer.apply {
            instances = this@InstanceTail.instances
            tail()
        }
    }

    override fun projectEvaluated() {
        super.projectEvaluated()
        tailer.logFilter.excludeFile(tailer.incidentFilter)
    }

    companion object {
        const val NAME = "instanceTail"
    }
}
