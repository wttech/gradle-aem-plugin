package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class SyncTask : DefaultTask(){

    companion object {
        val NAME = "aemSync"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Check out then clean JCR content."
    }

    @TaskAction
    fun sync() {
        // TODO checkout then clean
    }
}