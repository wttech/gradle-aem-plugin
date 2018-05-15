package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
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
        VltRunner(project).clean()
        notifier.default("Cleaned JCR content", "Directory: ${config.contentPath}")
    }

}