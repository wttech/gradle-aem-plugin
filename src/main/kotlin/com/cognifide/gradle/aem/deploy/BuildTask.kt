package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class BuildTask : DefaultTask() {

    companion object {
        val NAME = "aemBuild"
    }

    init {
        group = AemTask.GROUP
        description = "Builds then uploads CRX package to AEM instance(s)."
    }

    @TaskAction
    fun build() {
        logger.info("Building project '${project.name}' at path '${project.path}' ended with success.")
    }

}
