package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class EnvironmentDestroy : AemDefaultTask() {

    init {
        description = "Destroys virtualized AEM environment."
    }

    companion object {
        const val NAME = "environmentDestroy"
    }
}