package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class RestartTask : AemDefaultTask() {

    init {
        description = "Turns off then on local AEM instance(s)."
    }

    companion object {
        const val NAME = "aemRestart"
    }

}