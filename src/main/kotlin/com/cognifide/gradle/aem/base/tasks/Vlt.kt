package com.cognifide.gradle.aem.base.tasks

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.base.vlt.VltRunner
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Vlt : AemDefaultTask() {

    init {
        description = "Execute any Vault command."
    }

    @Nested
    val vlt = VltRunner(project)

    fun options(configurer: VltRunner.() -> Unit) {
        vlt.apply(configurer)
    }

    @TaskAction
    open fun perform() {
        vlt.run()
        aem.notifier.notify("Executing Vault command", "Command '${vlt.commandEffective}' finished.")
    }

    companion object {
        const val NAME = "aemVlt"
    }
}