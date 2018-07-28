package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class ResetupTask : AemDefaultTask() {

    companion object {
        val NAME = "aemResetup"
    }

    init {
        description = "Destroys then sets up local AEM instance(s)."
    }

}