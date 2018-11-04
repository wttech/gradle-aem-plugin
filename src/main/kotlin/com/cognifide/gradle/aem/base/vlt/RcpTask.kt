package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class RcpTask : VltTask() {

    init {
        description = "Copy JCR content from one instance to another."
    }

    @Input
    var sourceInstance: Instance? = aem.instanceConcrete("source")

    @Input
    var targetInstance: Instance? = aem.instanceConcrete("target")

    @Input
    var paths: MutableMap<String, String> = run {
        aem.props.list("aem.rcp.paths").fold(mutableMapOf()) { r, path ->
            val parts = path.split("=").map { it.trim() }
            when (parts.size) {
                1 -> r[path] = path
                2 -> r[parts[0]] = parts[1]
                else -> throw VltException("RCP path has invalid format: $path")
            }
            r
        }
    }

    @Input
    var opts: String = run { aem.props.string("aem.rcp.opts") ?: "-b 100 -r -u" }

    @TaskAction
    override fun perform() {
        if (paths.isEmpty()) {
            throw VltException("No paths to copy using RCP. Ensure specified param '-Paem.rcp.paths")
        }

        if (sourceInstance == null) {
            throw VltException("Source RCP instance is not defined. Ensure specified param '-Paem.instance.left'")
        }

        if (targetInstance == null) {
            throw VltException("Target RCP instance is not defined. Ensure specified param '-Paem.instance.right'")
        }

        paths.forEach { sourcePath, targetPath ->
            vlt.apply {
                command = "rcp $opts ${sourceInstance!!.httpBasicAuthUrl}/crx/-/jcr:root$sourcePath ${targetInstance!!.httpBasicAuthUrl}/crx/-/jcr:root$targetPath"
                run()
            }
        }

        if (!sourceInstance!!.cmd && !targetInstance!!.cmd) {
            aem.notifier.notify("RCP finished", "Copied ${paths.size} JCR root(s) from instance ${sourceInstance!!.name} to ${targetInstance!!.name}.")
        } else {
            aem.notifier.notify("RCP finished", "Copied ${paths.size} JCR root(s) between instances.")
        }
    }

    companion object {
        const val NAME = "aemRcp"
    }

}