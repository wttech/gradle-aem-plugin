package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.InstanceTask
import com.cognifide.gradle.aem.instance.tail.InstanceTailer
import com.cognifide.gradle.aem.instance.tail.TailOptions
import java.io.File
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@UseExperimental(ObsoleteCoroutinesApi::class)
open class Tail : InstanceTask() {

    @Internal
    val options = TailOptions(aem, name)

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

    @TaskAction
    fun tail() {
        InstanceTailer(options, instances).tail()
    }

    fun options(options: TailOptions.() -> Unit) {
        this.options.apply(options)
    }

    override fun projectEvaluated() {
        super.projectEvaluated()

        File(options.incidentFilterPath).apply {
            options.incidentFilter.excludeFile(this)
        }
    }

    companion object {
        const val NAME = "aemTail"
    }
}
