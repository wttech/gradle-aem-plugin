package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CleanTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemClean"
    }

    @Input
    final override val config = AemConfig.extend(project)

    init {
        group = AemPlugin.TASK_GROUP
        description = "Clean checked out JCR content."
    }

    @TaskAction
    fun clean() {
        logger.info("Cleaning checked out JCR content")
        VltCleaner.clean(project, config)
    }

}