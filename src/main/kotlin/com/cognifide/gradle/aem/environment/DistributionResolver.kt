package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import java.io.File

class DistributionResolver(val environment: Environment) : FileResolver(environment.aem, downloadDir(environment)) {

    fun file(path: String) = File(environment.rootDir, "${Environment.DISTRIBUTIONS_DIR}/$path")

    companion object {
        private fun downloadDir(environment: Environment): File {
            return AemTask.temporaryDir(environment.aem.project, "environment", Environment.DISTRIBUTIONS_DIR)
        }
    }

}
