package com.cognifide.gradle.aem.base.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class Sync : AemDefaultTask() {

    companion object {
        val NAME = "aemSync"
    }

    init {
        description = "Check out then clean JCR content."
    }
}