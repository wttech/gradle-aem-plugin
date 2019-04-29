package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.tooling.vlt.VltRunner
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Vlt : AemDefaultTask() {

    init {
        description = "Execute any Vault command."
    }

    @Internal
    val vlt = VltRunner(aem)

    fun options(configurer: VltRunner.() -> Unit) {
        vlt.apply(configurer)
    }

    @TaskAction
    open fun perform() {
        vlt.run()
        aem.notifier.notify("Executing Vault command", "Command '${vlt.commandEffective}' finished.")
    }

    companion object {
        const val NAME = "sync"
    }
}