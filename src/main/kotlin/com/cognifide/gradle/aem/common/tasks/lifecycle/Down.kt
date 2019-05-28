package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.AemDefaultTask

open class Down : AemDefaultTask() {

    init {
        description = "Turns off AEM local instances and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "down"
    }
}