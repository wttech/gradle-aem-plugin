package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.tasks.TaskAction

// TODO https://github.com/Cognifide/gradle-aem-plugin/issues/135
open class CheckoutTask : AemDefaultTask() {

    companion object {
        val NAME = "aemCheckout"
    }

    init {
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    fun checkout() {
        val runner = VltRunner(project)

        runner.checkout()
        notifier.default("Checked out JCR content", "Instance: ${runner.checkoutInstance.name}. Directory: ${Formats.projectPath(config.contentPath, project)}")
    }

}