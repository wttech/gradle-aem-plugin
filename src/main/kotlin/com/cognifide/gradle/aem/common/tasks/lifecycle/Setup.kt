package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.common.AemDefaultTask

open class Setup : AemDefaultTask() {

    init {
        description = "Sets up local AEM instance(s) and/or virtualized AEM environment."
    }

    companion object {
        const val NAME = "setup"
    }
}