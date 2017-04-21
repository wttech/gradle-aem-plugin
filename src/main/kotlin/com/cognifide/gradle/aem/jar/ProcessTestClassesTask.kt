package com.cognifide.gradle.aem.jar

import org.gradle.api.tasks.SourceSet

open class ProcessTestClassesTask : AbstractClassesTask() {

    override val sourceSet: SourceSet
        get() = getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

    companion object {
        val NAME = "aemProcessTestClasses"
    }

}