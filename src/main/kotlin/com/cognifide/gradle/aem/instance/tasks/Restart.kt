package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.api.AemDefaultTask

open class Restart : AemDefaultTask() {

    init {
        description = "Turns off then on local AEM instance(s)."
    }

    companion object {
        const val NAME = "aemRestart"
    }
}