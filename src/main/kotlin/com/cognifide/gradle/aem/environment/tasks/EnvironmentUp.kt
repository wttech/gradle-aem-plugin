package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.EnvironmentException
import org.gradle.api.tasks.TaskAction

open class EnvironmentUp : EnvironmentTask() {

    init {
        description = "Turn on additional services for local environment " +
                "- based on provided docker compose file."
    }

    @TaskAction
    override fun perform() {
        super.perform()

        removeStackIfDeployed()
        deployStack()
        val unavailableServices = serviceChecker.checkForUnavailableServices()
        if (unavailableServices.isNotEmpty()) {
            throw EnvironmentException("Services verification failed! URLs are unavailable or returned different response than expected:" +
                    "\n${unavailableServices.joinToString("\n")}")
        }
    }

    companion object {
        const val NAME = "environmentUp"
    }
}
