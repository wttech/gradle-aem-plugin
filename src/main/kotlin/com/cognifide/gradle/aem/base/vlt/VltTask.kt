package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class VltTask : AemDefaultTask() {

    init {
        description = "Execute any Vault command."
    }

    @TaskAction
    fun perform() {
        val command = project.properties["aem.vlt.command"] as String?
        if (command.isNullOrBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        VltRunner(project).raw(command)
        aem.notifier.notify("Executing Vault command", "Command '$command' finished.")
    }

    companion object {
        const val NAME = "aemVlt"
    }

}