package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.internal.Formats
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

open class CleanTask : AemDefaultTask() {

    @Internal
    private val runner = VltRunner(project)

    init {
        description = "Clean checked out JCR content."

        project.run {
            gradle.taskGraph.whenReady {
                if (it.hasTask(tasks.getByName(NAME))) {
                    tasks.getByName(config.syncTransferTaskName).doFirst {
                        runner.cleanBeforeCheckout()
                    }
                }
            }
        }
    }

    @TaskAction
    fun clean() {
        runner.cleanAfterCheckout()
        notifier.default("Cleaned JCR content", "Directory: ${Formats.rootProjectPath(config.contentPath, project)}")
    }

    fun settings(configurer: Closure<*>) {
        ConfigureUtil.configure(configurer, runner.cleaner)
    }

    companion object {
        const val NAME = "aemClean"
    }

}