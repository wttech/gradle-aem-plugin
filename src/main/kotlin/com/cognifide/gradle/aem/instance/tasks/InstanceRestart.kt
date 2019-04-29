package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class InstanceRestart : AemDefaultTask() {

    init {
        description = "Turns off then on local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceRestart"
    }
}