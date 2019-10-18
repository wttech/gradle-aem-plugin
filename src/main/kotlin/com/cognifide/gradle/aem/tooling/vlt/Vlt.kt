package com.cognifide.gradle.aem.tooling.vlt

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class Vlt : AemDefaultTask() {

    init {
        description = "Execute any Vault command."
    }

    fun options(configurer: VltClient.() -> Unit) {
        this.options = configurer
    }

    private var options: VltClient.() -> Unit = {}

    @TaskAction
    open fun run() {
        val summary = aem.vlt { run(); summary() }
        aem.notifier.notify("Executing Vault command", "Command '${summary.command}' finished." +
                " Duration: ${summary.durationString}")
    }

    companion object {
        const val NAME = "vlt"
    }
}
