package com.cognifide.gradle.sling.common.instance.provision.step

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.provision.ProvisionException
import org.apache.commons.io.FilenameUtils
import org.gradle.api.artifacts.Dependency

data class DeployPackageSource(val name: String, val version: String) {

    companion object {
        fun from(source: Any, sling: SlingExtension): DeployPackageSource = when (source) {
            is String -> fromUrlOrNotation(source)
            is Dependency -> fromDependency(source)
            else -> fromFile(source, sling)
        }

        fun fromDependency(dependency: Dependency): DeployPackageSource {
            if (dependency.version != null) {
                return DeployPackageSource(dependency.name, dependency.version!!)
            }

            throw fail(dependency)
        }

        fun fromUrlOrNotation(urlOrNotation: String): DeployPackageSource {
            if (URL_EXTENSIONS.any { urlOrNotation.endsWith(it) }) {
                val baseName = FilenameUtils.getBaseName(urlOrNotation)
                val version = URL_VERSION_PATTERNS.asSequence()
                        .mapNotNull { it.matchEntire(baseName)?.groupValues?.get(1) }
                        .firstOrNull()
                if (version != null) {
                    val name = baseName.substringBefore("-$version")
                    return DeployPackageSource(name, version)
                }
            } else if (DEPENDENCY_NOTATION.matches(urlOrNotation)) {
                val parts = urlOrNotation.substringBefore("@").split(":")
                return DeployPackageSource(parts[1], parts[2])
            }

            throw fail(urlOrNotation)
        }

        @Suppress("TooGenericExceptionCaught")
        fun fromFile(path: Any, sling: SlingExtension): DeployPackageSource = try {
            fromUrlOrNotation(sling.project.files(path).singleFile.absolutePath)
        } catch (e: Exception) {
            throw fail(path, e)
        }

        private fun fail(source: Any) = ProvisionException(failMessage(source))

        private fun fail(source: Any, e: Throwable) = ProvisionException(failMessage(source), e)

        private fun failMessage(source: Any) = "Package name cannot be derived from provided source '$source'! Please specify it explicitly."

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
