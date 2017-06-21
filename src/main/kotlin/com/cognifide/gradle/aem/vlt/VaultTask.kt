package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class VaultTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemVault"
    }

    @Input
    final override val config: AemConfig = AemConfig.of(project)

    init {
        group = AemPlugin.TASK_GROUP
        description = "Perform any Vault command."
    }

    @TaskAction
    fun perform() {
        val command = project.properties["aem.vlt.command"] as String?
        if (command.isNullOrBlank()) {
            throw VltException("Vault command is cannot be blank.")
        }

        logger.info("Performing Vault command: $command")

        VltCommand.raw(project, command!!)
    }

}