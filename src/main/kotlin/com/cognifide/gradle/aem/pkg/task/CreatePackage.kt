package com.cognifide.gradle.aem.pkg.task

class CreatePackage : AbstractPackage() {

    init {
        description = "Creates AEM package"
        project.afterEvaluate({ assemble(project) })
    }

    companion object {
        val NAME = "aemPackage"
    }
}
