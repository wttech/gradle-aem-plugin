package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvSetup : AemDefaultTask() {

    init {
        description = "Creates and turns on local AEM instance(s) and development environment."
    }

    companion object {
        const val NAME = "aemEnvSetup"
    }
}