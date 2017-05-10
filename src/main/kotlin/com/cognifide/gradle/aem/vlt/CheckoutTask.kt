package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CheckoutTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemCheckout"

        val COMMAND = "checkout"
    }

    @Input
    final override val config: AemConfig = AemConfig.extend(project)

    init {
        group = AemPlugin.TASK_GROUP
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    fun checkout() {
        logger.info("Checking out content from AEM")
        VltApp.checkout(project, config)
    }

}