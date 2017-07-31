package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class SetupTask : DefaultTask() {

    companion object {
        val NAME = "aemSetup"
    }

    init {
        group = AemTask.GROUP
        description = "Creates and turns on local AEM instance(s) with satisfied dependencies and application built."
    }

    @TaskAction
    fun setup() {
        logger.info("Setting up local AEM instance(s) ended with success.")
    }

}