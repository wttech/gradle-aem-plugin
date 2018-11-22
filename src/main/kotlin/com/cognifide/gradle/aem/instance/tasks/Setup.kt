package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.api.AemDefaultTask

open class Setup : AemDefaultTask() {

    init {
        description = "Creates and turns on local AEM instance(s) with satisfied dependencies and application built."
    }

    companion object {
        const val NAME = "aemSetup"
    }
}