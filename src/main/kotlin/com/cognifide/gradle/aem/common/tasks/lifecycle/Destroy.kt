package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.common.AemDefaultTask

open class Destroy : AemDefaultTask() {

    init {
        description = "Destroys local AEM instance(s) and virtualized AEM environment."
    }

    companion object {
        const val NAME = "destroy"
    }
}