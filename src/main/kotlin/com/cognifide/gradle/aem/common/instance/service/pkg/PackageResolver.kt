package com.cognifide.gradle.aem.common.instance.service.pkg

import org.gradle.api.Project

enum class PackageResolver {

    BY_COORDINATES() {
        override fun resolve(project: Project, response: ListResponse, expected: Package): Package? {
            if (expected.group.isBlank() && expected.name.isBlank()) {
                project.logger.info("Cannot find package, because expected group and name are blank.")
                return null
            }

            project.logger.info("Trying to find package by coordinates: ${expected.coordinates}")

            val result = response.results.find { result ->
                (result.group == expected.group) && (result.name == expected.name) && (result.version == expected.version)
            }

            if (result != null) {
                project.logger.info("Package found by coordinates.")
                return result
            } else {
                project.logger.info("Package cannot be found by coordinates.")
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
    };

    abstract fun resolve(project: Project, response: ListResponse, expected: Package): Package?
}