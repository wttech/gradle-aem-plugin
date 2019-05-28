package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentClean : AemDefaultTask() {

    init {
        description = "Cleans virtualized AEM environment by restarting HTTPD service & removing Dispatcher cache files."
    }

    @TaskAction
    fun clean() {
        aem.environment.httpd.restart()
        aem.environment.clean()
    }

    companion object {
        const val NAME = "environmentClean"
    }
}
