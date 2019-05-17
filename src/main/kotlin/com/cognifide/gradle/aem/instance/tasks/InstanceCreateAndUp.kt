package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class InstanceCreateAndUp : AemDefaultTask() {

    init {
        description = "Creates local AEM instance(s) and starts it"
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}