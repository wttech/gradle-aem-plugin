package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvironmentDestroy : AemDefaultTask() {

    init {
        description = "Destroy local AEM instance(s) and virtualized development environment."
    }

    companion object {
        const val NAME = "environmentDestroy"
    }
}