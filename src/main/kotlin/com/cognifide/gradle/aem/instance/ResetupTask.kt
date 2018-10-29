package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class ResetupTask : AemDefaultTask() {

    init {
        description = "Destroys then sets up local AEM instance(s)."
    }

    companion object {
        const val NAME = "aemResetup"
    }

}