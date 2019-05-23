package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.AemDefaultTask

open class Restart : AemDefaultTask() {

    init {
        description = "Turns off then on AEM local instances and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "restart"
    }
}