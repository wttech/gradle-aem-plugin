package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import kotlinx.coroutines.*
import org.gradle.api.tasks.TaskAction

@UseExperimental(ObsoleteCoroutinesApi::class)
open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Listens for HTTPD configuration file changes and reloads service deployed on AEM virtualized environment"
    }

    @TaskAction
    fun dev() {
        aem.environment.serviceReloader.start()
    }

    companion object {
        const val NAME = "environmentDev"
    }
}