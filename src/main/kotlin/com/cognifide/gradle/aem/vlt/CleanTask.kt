package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
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

    @TaskAction
    fun clean() {
        val cleaner = VltCleaner(config.contentPath, project.logger)

        cleaner.removeVltFiles()
        cleaner.cleanupDotContent(config.vaultSkipProperties)
    }

}