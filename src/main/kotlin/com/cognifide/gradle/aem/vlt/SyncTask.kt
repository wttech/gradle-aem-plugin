package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class SyncTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemSync"
    }

    @Nested
    final override val config: AemConfig = AemConfig.of(project)

    init {
        group = AemTask.GROUP
        description = "Check out then clean JCR content."
    }

    @TaskAction
    fun sync() {
        logger.info("Checking out content from AEM")
        VltCommand.checkout(project)

        logger.info("Cleaning checked out JCR content")
        VltCommand.clean(project)
    }
}