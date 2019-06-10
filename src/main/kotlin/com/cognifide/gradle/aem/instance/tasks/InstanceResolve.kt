package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import org.gradle.api.tasks.TaskAction

open class InstanceResolve : AemDefaultTask() {

    init {
        description = "Resolve instance files from remote sources before running other tasks"
    }

    @TaskAction
    fun resolve() {
        logger.info("Resolving CRX packages for satisfying instances")
        logger.info("Resolved CRX packages:\n${instanceSatisfy.allFiles.joinToString("\n")}")

        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files:\n${aem.localInstanceManager.sourceFiles.joinToString("\n")}")
    }

    private val instanceSatisfy: InstanceSatisfy
        get() = aem.tasks.named<InstanceSatisfy>(InstanceSatisfy.NAME).get()

    companion object {
        const val NAME = "instanceResolve"
    }
}