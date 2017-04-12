package com.cognifide.gradle.aem.pkg

open class CreateTask : PackageTask() {

    companion object {
        val NAME = "aemCreate"
    }

    init {
        description = "Creates AEM package"
        project.afterEvaluate({ assemble(project) })
    }
}
