package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentReload : AemDefaultTask() {

    init {
        description = "Reloads virtualized AEM environment (e.g reloads HTTPD configuration and cleans AEM Dispatcher cache files)"
    }

    @TaskAction
    fun reload() {
        aem.environment.reload()

        aem.notifier.lifecycle("Environment reloaded", "Reloaded with success")
    }

    companion object {
        const val NAME = "environmentReload"
    }
}
