package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PullTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemPull"
    }

    override val config: AemConfig
        get() = AemConfig.extendFromGlobal(project)

    @TaskAction
    fun pull() {
        // TODO ...
    }

}