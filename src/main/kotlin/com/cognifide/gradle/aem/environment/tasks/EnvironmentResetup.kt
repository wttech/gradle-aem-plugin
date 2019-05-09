package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvironmentResetup : AemDefaultTask() {

    init {
        description = "Destroys then sets up virtualized development environment."
    }

    companion object {
        const val NAME = "environmentResetup"
    }
}