package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask

open class InstanceBackupAndUp : AemDefaultTask() {

    init {
        description = "Turns off local instance(s), archives to ZIP file, then turns on again."
    }

    companion object {
        const val NAME = "instanceBackup"
    }
}