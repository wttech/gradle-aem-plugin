package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.pkg.tasks.vlt.VltClient
import org.gradle.api.tasks.TaskAction

open class PackageVlt : AemDefaultTask() {

    init {
        description = "Execute any Vault command."
    }

    fun options(configurer: VltClient.() -> Unit) {
        this.options = configurer
    }

    private var options: VltClient.() -> Unit = {}

    @TaskAction
    open fun run() {
        val summary = aem.vlt { options(); run() }
        aem.notifier.notify(
                "Executing Vault command",
                "Command '${summary.command}' finished. Duration: ${summary.durationString}"
        )
    }

    companion object {
        const val NAME = "packageVlt"
    }
}
