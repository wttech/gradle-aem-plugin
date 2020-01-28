package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.pkg.vlt.VltClient
import org.gradle.api.tasks.TaskAction

open class PackageVlt : AemDefaultTask() {

    fun options(configurer: VltClient.() -> Unit) {
        this.options = configurer
    }

    private var options: VltClient.() -> Unit = {}

    @TaskAction
    open fun run() = aem.vlt {
        aem.prop.string("package.vlt.command")?.let { command = it }
        aem.prop.string("package.vlt.path")?.let { contentRelativePath = it }

        options()

        val summary = run()

        common.notifier.notify(
                "Executing Vault command",
                "Command '${summary.command}' finished. Duration: ${summary.durationString}"
        )
    }

    init {
        description = "Execute any Vault command."
    }

    companion object {
        const val NAME = "packageVlt"
    }
}
