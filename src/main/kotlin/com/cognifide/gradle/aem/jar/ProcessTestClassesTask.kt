package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.SourceSet

open class ProcessTestClassesTask : AbstractClassesTask() {

    override val sourceSet: SourceSet
        get() = getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

    companion object {
        val NAME = "aemProcessTestClasses"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Process compiled classes of test source set."
    }

}