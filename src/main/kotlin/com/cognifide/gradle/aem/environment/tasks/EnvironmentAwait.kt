package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentAwait : AemDefaultTask() {

    init {
        description = "Await for healthy condition of virtualized AEM environment."
    }

    @TaskAction
    fun await() {
        aem.environment.check()
    }

    companion object {
        const val NAME = "environmentAwait"
    }
}
