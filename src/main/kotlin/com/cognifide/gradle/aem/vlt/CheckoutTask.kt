package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CheckoutTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemCheckout"

        val COMMAND = "checkout"
    }

    @Input
    var params = mutableListOf("--force")

    @Input
    final override val config: AemConfig = AemConfig.extend(project)

    init {
        group = AemPlugin.TASK_GROUP
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    fun checkout() {
        val instance = AemInstance.filter(project, config).first()
        val vltApp = VltApp(instance, config.determineContentPath(project), project.logger)

        val filter = File(config.vaultFilterPath)
        if (filter.exists()) {
            vltApp.executeCommand(COMMAND, listOf("-f", filter.absolutePath) + params)
        } else {
            vltApp.executeCommand(COMMAND, params)
        }
    }

}