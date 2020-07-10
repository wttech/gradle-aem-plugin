package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.SlingDefaultTask

open class InstanceSetup : SlingDefaultTask() {

    init {
        description = "Creates and turns on local Sling instance(s) with satisfied dependencies and application built."
    }

    companion object {
        const val NAME = "instanceSetup"
    }
}
