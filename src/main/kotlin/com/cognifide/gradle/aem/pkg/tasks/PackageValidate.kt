package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.pkg.PackageValidator
import org.gradle.api.tasks.*

open class PackageValidate : AemDefaultTask() {

    @InputFiles
    val packages = aem.obj.files()

    @Nested
    val validator = PackageValidator(aem).apply {
        workDir.convention(aem.obj.buildDir(name))
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
        enabled = aem.prop.boolean("package.validate.enabled") ?: false
    }

    companion object {
        const val NAME = "packageValidate"
    }
}
