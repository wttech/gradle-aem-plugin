package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class CheckoutTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemCheckout"
    }

    @Nested
    final override val config: AemConfig = AemConfig.of(project)

    init {
        group = AemTask.GROUP
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    fun checkout() {
        logger.info("Checking out content from AEM")
        VltCommand(project).checkout()
    }

}