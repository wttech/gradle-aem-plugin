package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class RestartTask : AemDefaultTask() {

    companion object {
        val NAME = "aemRestart"
    }

    init {
        description = "Turns off then on local AEM instance(s)."
    }

}