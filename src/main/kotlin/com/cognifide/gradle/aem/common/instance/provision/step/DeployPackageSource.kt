package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.provision.InstanceStep
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import org.apache.commons.io.FilenameUtils
import org.gradle.api.artifacts.Dependency

data class DeployPackageSource(val name: String, val version: String = InstanceStep.VERSION_DEFAULT) {

    companion object {
        fun from(source: Any, aem: AemExtension): DeployPackageSource = when (source) {
            is String -> fromUrlOrNotation(source)
            is Dependency -> fromDependency(source)
            else -> fromFile(source, aem)
        }

        fun fromDependency(dependency: Dependency): DeployPackageSource = when {
            dependency.version != null -> DeployPackageSource(dependency.name, dependency.version!!)
            else -> DeployPackageSource(dependency.name)
        }

        fun fromUrlOrNotation(urlOrNotation: String): DeployPackageSource {
            if (URL_EXTENSIONS.any { urlOrNotation.endsWith(it) }) {
                val baseName = FilenameUtils.getBaseName(urlOrNotation)
                val version = URL_VERSION_PATTERNS.asSequence()
                        .mapNotNull { it.matchEntire(baseName)?.groupValues?.get(1) }
                        .firstOrNull()
                return when {
                    version != null -> DeployPackageSource(baseName.substringBefore("-$version"), version)
                    else -> DeployPackageSource(baseName)
                }
            } else if (DEPENDENCY_NOTATION.matches(urlOrNotation)) {
                val parts = urlOrNotation.substringBefore("@").split(":")
                return DeployPackageSource(parts[1], parts[2])
            }

            throw fail(urlOrNotation)
        }

        @Suppress("TooGenericExceptionCaught")
        fun fromFile(path: Any, aem: AemExtension): DeployPackageSource = try {
            fromUrlOrNotation(aem.project.files(path).singleFile.absolutePath)
        } catch (e: Exception) {
            throw fail(path, e)
        }

        private fun fail(source: Any) = ProvisionException(failMessage(source))

        private fun fail(source: Any, e: Throwable) = ProvisionException(failMessage(source), e)

        private fun failMessage(source: Any) = "Package name or version cannot be derived from provided source '$source'! Please specify it explicitly."

        private val DEPENDENCY_NOTATION = "[^:]+:[^:]+:[^:]+".toRegex()

        private val URL_EXTENSIONS = listOf(".zip", ".jar")

        private val URL_VERSION_PATTERNS = listOf(
                ".*-(\\d+.\\d+.\\d+[-|.]\\w+)",
                ".*-(\\d+.\\d+.\\w+)",
                ".*-(\\d+.\\d+)",
                ".*-(\\d+.\\d+).*"
        ).map { it.toRegex() }
    }
}
