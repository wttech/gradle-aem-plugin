package com.cognifide.gradle.aem.base.debug

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class DebugTask : AemDefaultTask() {

    companion object {
        val NAME = "aemDebug"
    }

    @OutputFile
    val file = AemTask.temporaryFile(project, NAME, "debug.json")

    init {
        description = "Dumps effective AEM build configuration of project to JSON file"

        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun debug() {
        logger.lifecycle("Dumping AEM build configuration of $project to file: $file")

        val props = ProjectDumper(project).properties
        val json = Formats.toJson(props)

        file.bufferedWriter().use { it.write(json) }

        notifier.default("Configuration dumped", "For $project to file: ${Formats.projectPath(file, project)}")
    }

}
