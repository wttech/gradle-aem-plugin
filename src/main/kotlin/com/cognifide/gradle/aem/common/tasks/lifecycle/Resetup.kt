package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.AemDefaultTask

open class Resetup : AemDefaultTask() {

    init {
        description = "Destroys then sets up local AEM instance(s) and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "resetup"
    }
}
