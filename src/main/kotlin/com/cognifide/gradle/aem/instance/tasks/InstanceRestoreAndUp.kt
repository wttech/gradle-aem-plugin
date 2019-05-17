package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class InstanceRestoreAndUp : AemDefaultTask() {

    init {
        description = "Restores AEM instance(s) from backup file and starts it."
    }

    companion object {
        const val NAME = "instanceRestore"
    }
}