package com.cognifide.gradle.aem.debug

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class DebugTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemDebug"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @OutputFile
    val file = AemTask.temporaryFile(project, NAME, "debug.json")

    init {
        group = AemTask.GROUP
        description = "Dumps effective AEM build configuration of project to JSON format"
    }

    @TaskAction
    fun debug() {
        logger.info("Dumping AEM build configuration of project '${project.name}' at path '${project.path}' to file: ${file.absolutePath}")

        val props = ProjectDumper(project).properties
        val json = Formats.toJson(props)

        file.bufferedWriter().use { it.write(json) }
    }

}
