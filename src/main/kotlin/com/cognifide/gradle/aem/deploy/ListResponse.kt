package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.BooleanUtils
import org.gradle.api.Project

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    companion object {
        fun fromJson(json: String): ListResponse {
            return ObjectMapper().readValue(json, ListResponse::class.java)
        }
    }

    lateinit var results: List<Package>

    fun resolvePackage(project: Project, expected: Package): Package? {
        return PackageResolver.values()
                .asSequence()
                .map { it.resolve(project, this, expected) }
                .firstOrNull { it != null }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Package {
        lateinit var path: String

        lateinit var downloadName: String

        lateinit var group: String

        lateinit var name: String

        lateinit var version: String

        var conventionPaths = listOf<String>()

        constructor(project: Project) {
            this.group = project.group.toString()
            this.name = PropertyParser(project).name
            this.version = project.version.toString()
            this.downloadName = "$name-${project.version}.zip"
            this.conventionPaths = listOf(
                    "/etc/packages/${group}/${(project.tasks.getByName(ComposeTask.NAME) as ComposeTask).archiveName}",
                    "/etc/packages/${group}/$name-${project.version}.zip"
            )
        }

        constructor(group: String, name: String, version: String) {
            this.group = group
            this.name = name
            this.version = version
        }

        val props: String
            get() = "[group=${group}][name=$name][version=$version]"

        val installed: Boolean
            get() = false // TODO implement this

    }

    enum class PackageResolver {

        BY_PROJECT_PROPS() {
            override fun resolve(project: Project, response: ListResponse, expected: Package): Package? {
                project.logger.info("Trying to find package by project properties: ${expected.props}")

                val result = response.results.find { result ->
                    (result.group == project.group) && (result.name == expected.name) && (result.version == project.version)
                }

                if (result != null) {
                    project.logger.info("Package found by project properties: ${expected.props}")
                    return result
                } else {
                    project.logger.info("Package cannot be found by project properties: ${expected.props}")
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
                if (BooleanUtils.toBoolean(project.properties.getOrElse("aem.deploy.skipDownloadName", { "true" }) as String?)) {
                    project.logger.info("Finding package by download name '${expected.downloadName}' is skipped.")
                } else {
                    project.logger.warn("Trying to find package by download name '${expected.downloadName}' which can collide with other packages.")

                    val result = response.results.find { result -> result.downloadName == expected.downloadName }
                    if (result != null) {
                        project.logger.info("Package found by download name.")

                        return result
                    } else {
                        project.logger.info("Package cannot be found by download name.")
                    }
                }

                return null
            }
        };

        abstract fun resolve(project: Project, response: ListResponse, expected: Package): Package?

    }

}
