package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class CheckoutTask : AemDefaultTask() {

    companion object {
        val NAME = "aemCheckout"
    }

    init {
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    fun checkout() {
        logger.info("Checking out content from AEM")
        VltCommand(project).checkout()
    }

}