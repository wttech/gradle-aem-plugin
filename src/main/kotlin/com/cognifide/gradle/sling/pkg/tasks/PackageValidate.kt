package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.common.pkg.PackageValidator
import org.gradle.api.tasks.*

open class PackageValidate : SlingDefaultTask() {

    @InputFiles
    val packages = sling.obj.files()

    @Nested
    val validator = PackageValidator(sling).apply {
        workDir.convention(sling.obj.buildDir(name))
    }

    fun validator(options: PackageValidator.() -> Unit) {
        validator.apply(options)
    }

    @TaskAction
    fun validate() {
        validator.perform(packages.files)
    }

    init {
        description = "Validates built CRX package"
    }

    companion object {
        const val NAME = "packageValidate"
    }
}
