package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask

open class InstanceResetup : AemDefaultTask() {

    init {
        description = "Destroys then sets up local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceResetup"
    }
}