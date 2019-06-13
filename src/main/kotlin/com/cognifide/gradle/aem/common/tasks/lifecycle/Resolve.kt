package com.cognifide.gradle.aem.common.tasks.lifecycle

import com.cognifide.gradle.aem.AemDefaultTask

open class Resolve : AemDefaultTask() {

    init {
        description = "Resolve all files from remote sources before running other tasks."
    }

    companion object {
        const val NAME = "resolve"
    }
}