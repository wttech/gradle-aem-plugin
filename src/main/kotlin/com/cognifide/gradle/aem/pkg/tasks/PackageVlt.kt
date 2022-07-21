package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.pkg.vault.VaultClient
import org.gradle.api.tasks.TaskAction

open class PackageVlt : AemDefaultTask() {

    fun options(configurer: VaultClient.() -> Unit) {
        this.options = configurer
    }

    private var options: VaultClient.() -> Unit = {}

    @TaskAction
    open fun run() = aem.vlt {
        aem.prop.string("package.vlt.command")?.let { command.set(it) }
        aem.prop.string("package.vlt.path")?.let { contentRelativePath.set(it) }

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
