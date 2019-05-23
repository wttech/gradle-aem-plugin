package com.cognifide.gradle.aem.tooling.rcp.tasks

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.rcp.RcpClient
import com.cognifide.gradle.aem.tooling.vlt.VltException
import com.cognifide.gradle.aem.tooling.vlt.tasks.Vlt
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Rcp : Vlt() {

    init {
        description = "Copy JCR content from one instance to another."
    }

    @Internal
    var paths: Map<String, String> = mapOf()

    @Internal
    var sourceInstance: Instance? = aem.props.string("rcp.source")?.run { aem.instance(this) }

    @Internal
    var targetInstance: Instance? = aem.props.string("rcp.target")?.run { aem.instance(this) }

    @Internal
    var opts: String = aem.props.string("rcp.opts") ?: "-b 100 -r -u"

    @TaskAction
    override fun perform() {
        val client = createClient()
        eachPathMapping { (sourcePath, targetPath) -> client.copy(sourcePath, targetPath) }
        val summary = client.summary

        logger.info("RCP details: $summary")

        if (!summary.source.cmd && !summary.target.cmd) {
            aem.notifier.notify("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) from instance ${summary.source.name} to ${summary.target.name}.")
        } else {
            aem.notifier.notify("RCP finished", "Copied ${summary.copiedPaths} JCR root(s) between instances.")
        }
    }

    private fun createClient(): RcpClient {
        if (sourceInstance == null) {
            throw VltException("Source RCP instance is not defined. Ensure specified param '-Pinstance.source'")
        }

        if (targetInstance == null) {
            throw VltException("Target RCP instance is not defined. Ensure specified param '-Pinstance.target'")
        }

        return RcpClient(vlt, sourceInstance!!, targetInstance!!).apply {
            this@apply.opts = this@Rcp.opts
        }
    }

    private fun eachPathMapping(action: (Pair<String, String>) -> Unit) {
        if (paths.isNotEmpty()) {
            paths.map { Pair(it.key, it.value) }.forEach(action)
            return
        }

        val cmdPaths = aem.props.list("rcp.paths") ?: listOf()
        if (cmdPaths.isNotEmpty()) {
            cmdPaths.asSequence().map { pathMapping(it) }.forEach(action)
            return
        }

        val cmdFilePath = aem.props.string("rcp.pathsFile")
        if (!cmdFilePath.isNullOrBlank()) {
            val cmdFile = File(cmdFilePath)
            if (!cmdFile.exists()) {
                throw VltException("RCP paths file does not exist: $cmdFile")
            }

            cmdFile.useLines { line -> line.map { pathMapping(it) }.forEach(action) }
            return
        }

        throw VltException("RCP param '-Prcp.paths' or '-Prcp.pathsFile' must be specified.")
    }

    private fun pathMapping(path: String): Pair<String, String> {
        val parts = path.trim().split("=").map { it.trim() }

        return when (parts.size) {
            1 -> Pair(path, path)
            2 -> Pair(parts[0], parts[1])
            else -> throw VltException("RCP path has invalid format: $path")
        }
    }

    companion object {
        const val NAME = "rcp"
    }
}