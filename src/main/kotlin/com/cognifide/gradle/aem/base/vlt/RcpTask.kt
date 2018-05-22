package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class RcpTask : AemDefaultTask() {

    companion object {
        val NAME = "aemRcp"
    }

    init {
        description = "Copy JCR content from one remote instance to another."
    }

    @TaskAction
    fun rcp() {
        val runner = VltRunner(project)
        runner.rcp()
        notifier.default("Copied JCR content") // TODO better message including args
    }

}