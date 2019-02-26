package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvDestroy : AemDefaultTask() {

    init {
        description = "Destroys local AEM instance(s) and virtualized development environment."
    }

    companion object {
        const val NAME = "aemEnvDestroy"
    }
}