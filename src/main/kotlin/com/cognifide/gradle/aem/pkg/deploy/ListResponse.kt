package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemConfig
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.Project
import java.io.InputStream

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    lateinit var results: List<Package>

    fun resolvePackage(project: Project, expected: Package): Package? {
        return PackageResolver.values()
                .asSequence()
                .map { it.resolve(project, this, expected) }
                .firstOrNull { it != null }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Package private constructor() {
        lateinit var group: String

        lateinit var name: String

        lateinit var version: String

        lateinit var path: String

        lateinit var downloadName: String

        @get:JsonIgnore
        var conventionPaths = listOf<String>()

        var lastUnpacked: Long? = null

        constructor(project: Project) : this() {
            this.group = project.group.toString()
            this.name = AemConfig.of(project).packageName
            this.version = project.version.toString()

            this.downloadName = "$name-${project.version}.zip"
            this.conventionPaths = listOf(
                    "/etc/packages/$group/${AemConfig.pkg(project).archiveName}",
                    "/etc/packages/$group/$name-$version.zip"
            )
        }

        constructor(group: String, name: String, version: String) : this() {
            this.group = group
            this.name = name
            this.version = version

            this.path = ""
            this.downloadName = ""
            this.conventionPaths = listOf("/etc/packages/$group/$name-$version.zip")
        }

        @get:JsonIgnore
        val props: String
            get() = "[group=$group][name=$name][version=$version]"

        val installed: Boolean
            get() = lastUnpacked != null && lastUnpacked!! > 0

    }

    enum class PackageResolver {

        BY_PROJECT_PROPS() {
            override fun resolve(project: Project, response: ListResponse, expected: Package): Package? {
                if (expected.group.isBlank() && expected.name.isBlank()) {
                    project.logger.info("Cannot find package, because expected group and name are blank.")
                    return null
                }

                project.logger.info("Trying to find package by project properties: ${expected.props}")

                val result = response.results.find { result ->
                    (result.group == expected.group) && (result.name == expected.name) && (result.version == expected.version)
                }

                if (result != null) {
                    project.logger.info("Package found by project properties.")
                    return result
                } else {
                    project.logger.info("Package cannot be found by project properties.")
                }

                return null
            }

        },
        BY_CONVENTION_PATH() {
            override fun resolve(project: Project, response: ListResponse, expected: Package): Package? {
                for (conventionPath in expected.conventionPaths) {
                    project.logger.info("Trying to find package by convention path '$conventionPath'.")

                    val result = response.results.find { result -> result.path == conventionPath }
                    if (result != null) {
                        project.logger.info("Package found by convention path.")

                        return result
                    } else {
                        project.logger.info("Package cannot be found by convention path.")
                    }
                }

                return null
            }
        },
        BY_DOWNLOAD_NAME() {
            override fun resolve(project: Project, response: ListResponse, expected: Package): Package? {
                val config = AemConfig.of(project)

                if (config.packageSkipDownloadName) {
                    project.logger.debug("Finding package by download name '${expected.downloadName}' is skipped.")
                    return null
                }

                if (expected.downloadName.isBlank()) {
                    project.logger.info("Cannot find package, because expected download name is blank.")
                    return null
                }

                project.logger.warn("Trying to find package by download name '${expected.downloadName}' which can collide with other packages.")

                val result = response.results.find { result -> result.downloadName == expected.downloadName }
                if (result != null) {
                    project.logger.info("Package found by download name.")

                    return result
                } else {
                    project.logger.info("Package cannot be found by download name.")
                }

                return null
            }
        };

        abstract fun resolve(project: Project, response: ListResponse, expected: Package): Package?

    }

    companion object {
        fun fromJson(json: InputStream): ListResponse {
            return try {
                ObjectMapper().readValue(json, ListResponse::class.java)
            } catch (e: Exception) {
                throw ResponseException("Malformed package list response.")
            }
        }
    }

}
