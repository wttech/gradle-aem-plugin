package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.AemDefaultTask

open class Await : AemDefaultTask() {

    init {
        description = "Await for healthy condition of AEM instances and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "await"
    }
}
