package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class CleanTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemClean"
    }

    @Nested
    final override val config = AemConfig.of(project)

    init {
        group = AemPlugin.TASK_GROUP
        description = "Clean checked out JCR content."
    }

    @TaskAction
    fun clean() {
        logger.info("Cleaning checked out JCR content")
        VltCommand.clean(project)
    }

}