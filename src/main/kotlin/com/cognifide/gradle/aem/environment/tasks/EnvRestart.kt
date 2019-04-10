package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvRestart : AemDefaultTask() {

    init {
        description = "Restart virtualized development environment."
    }

    companion object {
        const val NAME = "aemEnvRestart"
    }
}