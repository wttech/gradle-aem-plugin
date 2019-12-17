package com.cognifide.gradle.aem.instance.rcp

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class InstanceRcp : AemDefaultTask() {

    init {
        description = "Copy JCR content from one instance to another."
    }

    fun options(configurer: RcpClient.() -> Unit) {
        this.options = configurer
    }

    private var options: RcpClient.() -> Unit = {}

    @TaskAction
    fun run() {
        val summary = aem.rcp { options(); copy(); summary() }

        logger.info("RCP details: $summary")

        if (!summary.source.cmd && !summary.target.cmd) {
            aem.notifier.notify("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) from instance ${summary.source.name} to ${summary.target.name}." +
                    "Duration: ${summary.durationString}")
        } else {
            aem.notifier.notify("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) between instances." +
                    "Duration: ${summary.durationString}")
        }
    }

    companion object {
        const val NAME = "instanceRcp"
    }
}
