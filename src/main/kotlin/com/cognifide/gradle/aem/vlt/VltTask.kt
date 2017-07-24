package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class VltTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemVlt"
    }

    @Nested
    final override val config: AemConfig = AemConfig.of(project)

    init {
        group = AemTask.GROUP
        description = "Execute any Vault command."
    }

    @TaskAction
    fun perform() {
        val command = project.properties["aem.vlt.command"] as String?
        if (command.isNullOrBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        VltCommand(project).raw(command!!)
    }

}