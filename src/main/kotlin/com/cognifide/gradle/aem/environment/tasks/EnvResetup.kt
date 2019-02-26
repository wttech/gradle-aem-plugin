package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvResetup : AemDefaultTask() {

    init {
        description = "Recreates and turns on local AEM instance(s) and virtualized development environment."
    }

    companion object {
        const val NAME = "aemEnvResetup"
    }
}