package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class CleanTask : AemDefaultTask() {

    @Internal
    private val runner = VltRunner(project)

    init {
        description = "Clean checked out JCR content."

        beforeExecuted(config.syncTransferTaskName) { runner.cleanBeforeCheckout() }
    }

    @TaskAction
    fun clean() {
        runner.cleanAfterCheckout()
        aem.notifier.default("Cleaned JCR content", "Directory: ${Formats.rootProjectPath(aem.compose.contentPath, project)}")
    }

    fun settings(configurer: VltCleaner.() -> Unit) {
        runner.cleaner.apply(configurer)
    }

    companion object {
        const val NAME = "aemClean"
    }

}