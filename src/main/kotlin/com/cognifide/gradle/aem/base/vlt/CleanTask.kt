package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.base.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class CleanTask : AemDefaultTask() {

    companion object {
        val NAME = "aemClean"
    }

    init {
        description = "Clean checked out JCR content."
    }

    @TaskAction
    fun clean() {
        logger.info("Cleaning checked out JCR content")
        VltCommand(project).clean()
    }

}