package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.tasks.Zip
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.execution.TaskExecutionGraph

open class Backup : Zip(), AemTask {

    init {
        description = "Turns off local instance(s), archives to ZIP file, then turns on again."
        baseName = "${project.rootProject.name}-${Formats.dateFileName()}"
        classifier = "backup"
    }

    val available: List<File>
        get() {
            return destinationDir.listFiles { _, name -> name.endsWith("-$classifier.$extension") }
                    .ifEmpty { arrayOf() }.toList()
        }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            val uncreated = aem.localHandles.filter { !it.created }
            if (uncreated.isNotEmpty()) {
                throw InstanceException("Cannot create backup of local instances, because there are instances not yet created: ${uncreated.names}")
            }
        }
    }

    override fun projectEvaluated() {
        from(aem.config.instanceRoot)
    }

    companion object {
        const val NAME = "aemBackup"
    }
}