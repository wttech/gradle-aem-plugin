package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.common.AemDefaultTask

open class Up : AemDefaultTask() {

    init {
        description = "Turns on AEM local instances and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "up"
    }
}