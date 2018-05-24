package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask

open class SyncTask : AemDefaultTask() {

    companion object {
        val NAME = "aemSync"
    }

    init {
        description = "Check out then clean JCR content."
    }
}