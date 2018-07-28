package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask

open class RebornTask : AemDefaultTask() {

    companion object {
        val NAME = "aemReborn"
    }

    init {
        description = "Destroys then sets up local AEM instance(s)."
    }

}