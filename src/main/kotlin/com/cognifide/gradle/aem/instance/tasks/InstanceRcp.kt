package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.rcp.RcpClient
import org.gradle.api.tasks.TaskAction

open class InstanceRcp : AemDefaultTask() {

    private val notifier = common.notifier

    fun options(configurer: RcpClient.() -> Unit) {
        this.options = configurer
    }

    private var options: RcpClient.() -> Unit = {}

    @TaskAction
    fun run() = aem.rcp {
        aem.prop.string("instance.rcp.source")?.run { sourceInstance = aem.instance(this) }
        aem.prop.string("instance.rcp.target")?.run { targetInstance = aem.instance(this) }
        aem.prop.list("instance.rcp.paths")?.let { paths = it }
        aem.prop.string("instance.rcp.pathsFile")?.let { pathsFile = aem.project.file(it) }
        aem.prop.string("instance.rcp.workspace")?.let { workspace = it }
        aem.prop.string("instance.rcp.opts")?.let { opts = it }

        options()

        copy()
        val summary = summary()

        logger.info("RCP details: $summary")

        if (!summary.source.cmd && !summary.target.cmd) {
            notifier.lifecycle(
                "RCP finished",
                "Copied ${summary.copiedPaths} JCR root(s) from instance ${summary.source.name} to ${summary.target.name}." +
                    "Duration: ${summary.durationString}"
            )
        } else {
            notifier.lifecycle(
                "RCP finished",
                "Copied ${summary.copiedPaths} JCR root(s) between instances." +
                    "Duration: ${summary.durationString}"
            )
        }
    }

    init {
        description = "Copy JCR content from one instance to another."
    }

    companion object {
        const val NAME = "instanceRcp"
    }
}
