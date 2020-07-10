package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.SlingDefaultTask

open class InstanceRestart : SlingDefaultTask() {

    init {
        description = "Turns off then on local Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceRestart"
    }
}
