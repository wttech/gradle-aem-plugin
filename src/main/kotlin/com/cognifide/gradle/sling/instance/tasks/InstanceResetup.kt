package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.SlingDefaultTask

open class InstanceResetup : SlingDefaultTask() {

    init {
        description = "Destroys then sets up local Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceResetup"
    }
}
