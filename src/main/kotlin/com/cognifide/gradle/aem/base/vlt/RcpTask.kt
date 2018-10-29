package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class RcpTask : AemDefaultTask() {

    companion object {
        val NAME = "aemRcp"
    }

    init {
        description = "Copy JCR content from instance to another."
    }

    @TaskAction
    fun rcp() {
        val runner = VltRunner(project)
        runner.rcp()

        if (!runner.rcpSourceInstance.cmd && !runner.rcpTargetInstance.cmd) {
            aem.notifier.default("RCP finished", "Copied ${runner.rcpPaths.size} JCR root(s) from instance ${runner.rcpSourceInstance.name} to ${runner.rcpTargetInstance.name}.")
        } else {
            aem.notifier.default("RCP finished", "Copied ${runner.rcpPaths.size} JCR root(s) between instances.")
        }
    }

}