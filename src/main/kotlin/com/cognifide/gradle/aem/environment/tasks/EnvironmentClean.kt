package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentClean : AemDefaultTask() {

    init {
        description = "Cleans virtualized AEM environment by removing Dispatcher cache files."
    }

    @TaskAction
    fun check() {
        aem.environment.clean()

        aem.notifier.notify("Environment cleaned", "Cleaned with success.")
    }

    companion object {
        const val NAME = "environmentClean"
    }
}
