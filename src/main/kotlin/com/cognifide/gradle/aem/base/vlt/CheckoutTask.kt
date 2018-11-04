package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CheckoutTask : VltTask() {

    @Input
    var instance = aem.instanceAny()

    @Input
    var filter = aem.filter()

    init {
        description = "Check out JCR content from running AEM instance."
    }

    @TaskAction
    override fun perform() {
        vlt.apply {
            command = "--credentials ${instance.credentials} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
        aem.notifier.notify("Checked out JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(vlt.contentDir, project)}")
    }

    companion object {
        const val NAME = "aemCheckout"
    }

}