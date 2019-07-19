package com.cognifide.gradle.aem.common.instance.service.pkg

enum class PackageResolver {

    BY_COORDINATES() {
        override fun resolve(response: ListResponse, expected: Package): Package? {
            if (expected.group.isBlank() && expected.name.isBlank()) {
                return null
            }

            val result = response.results.find { result ->
                (result.group == expected.group) && (result.name == expected.name) && (result.version == expected.version)
            }

            if (result != null) {
                return result
            }

            return null
        }
    },
    BY_CONVENTION_PATH() {
        override fun resolve(response: ListResponse, expected: Package): Package? {
            for (conventionPath in expected.conventionPaths) {

                val result = response.results.find { result -> result.path == conventionPath }
                if (result != null) {
                    return result
                }
            }

            return null
        }
    };

    abstract fun resolve(response: ListResponse, expected: Package): Package?
}