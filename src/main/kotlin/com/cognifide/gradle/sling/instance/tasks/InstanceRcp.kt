package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.common.instance.rcp.RcpClient
import org.gradle.api.tasks.TaskAction

open class InstanceRcp : SlingDefaultTask() {

    private val notifier = common.notifier

    fun options(configurer: RcpClient.() -> Unit) {
        this.options = configurer
    }

    private var options: RcpClient.() -> Unit = {}

    @TaskAction
    fun run() = sling.rcp {
        sling.prop.string("instance.rcp.source")?.run { sourceInstance = sling.instance(this) }
        sling.prop.string("instance.rcp.target")?.run { targetInstance = sling.instance(this) }
        sling.prop.list("instance.rcp.paths")?.let { paths = it }
        sling.prop.string("instance.rcp.pathsFile")?.let { pathsFile = sling.project.file(it) }
        sling.prop.string("instance.rcp.workspace")?.let { workspace = it }
        sling.prop.string("instance.rcp.opts")?.let { opts = it }

        options()

        copy()
        val summary = summary()

        logger.info("RCP details: $summary")

        if (!summary.source.cmd && !summary.target.cmd) {
            notifier.lifecycle("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) from instance ${summary.source.name} to ${summary.target.name}." +
                    "Duration: ${summary.durationString}")
        } else {
            notifier.lifecycle("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) between instances." +
                    "Duration: ${summary.durationString}")
        }
    }

    init {
        description = "Copy JCR content from one instance to another."
    }

    companion object {
        const val NAME = "instanceRcp"
    }
}
