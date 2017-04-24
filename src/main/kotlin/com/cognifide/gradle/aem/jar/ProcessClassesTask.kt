package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.SourceSet

open class ProcessClassesTask : AbstractClassesTask() {

    override val sourceSet: SourceSet
        get() = getSourceSet(SourceSet.MAIN_SOURCE_SET_NAME)

    companion object {
        val NAME = "aemProcessClasses"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Process compiled classes of main source set."
    }

}