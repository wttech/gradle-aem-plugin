package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask

open class EnvironmentRestart : AemDefaultTask() {

    init {
        description = "Restart virtualized development environment."
    }

    companion object {
        const val NAME = "environmentRestart"
    }
}
