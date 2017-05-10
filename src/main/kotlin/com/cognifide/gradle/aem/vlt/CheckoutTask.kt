package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CheckoutTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemCheckout"

        val COMMAND = "checkout"
    }

    override val config: AemConfig
        get() = AemConfig.extendFromGlobal(project)

    @TaskAction
    fun checkout() {
        val instance = AemInstance.filter(project, config).first()
        val vltApp = VltApp(instance, config.determineContentPath(project))

        //vltApp.setLogLevel(logging.level.name)

        val filter = File(config.vaultFilterPath)
        if (filter.exists()) {
            vltApp.executeCommand(COMMAND, listOf("-f", filter.absolutePath))
        } else {
            vltApp.executeCommand(COMMAND)
        }
    }

}