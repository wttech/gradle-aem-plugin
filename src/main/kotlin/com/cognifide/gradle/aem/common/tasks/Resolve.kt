package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import org.gradle.api.tasks.TaskAction

open class Resolve : AemDefaultTask() {

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build time" +
                " and fail fast on configuration error."
    }

    @TaskAction
    fun resolve() {
        logger.info("Resolving CRX packages for satisfying instances")
        logger.info("Resolved CRX packages: ${instanceSatisfy.allFiles}")

        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files: ${aem.localInstanceManager.sourceFiles}")
    }

    private val instanceSatisfy: InstanceSatisfy
        get() = aem.tasks.named<InstanceSatisfy>(InstanceSatisfy.NAME).get()

    companion object {
        const val NAME = "resolve"
    }
}