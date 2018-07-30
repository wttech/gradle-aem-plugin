package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class SetupTask : AemDefaultTask() {

    companion object {
        val NAME = "aemSetup"
    }

    init {
        description = "Creates and turns on local AEM instance(s) with satisfied dependencies and application built."
    }

}