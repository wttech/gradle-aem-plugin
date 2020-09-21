package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    lateinit var results: List<Package>

    fun resolve(group: String, name: String, version: String): Package? {
        return resolveByCoordinates(group, name, version) ?: resolveByConventionPath(group, name, version)
    }

    private fun resolveByCoordinates(group: String, name: String, version: String): Package? {
        if (group.isBlank() && name.isBlank()) {
            return null
        }

        val result = results.find { result ->
            (result.group == group) && (result.name == name) && (result.version == version)
        }

        if (result != null) {
            return result
        }

        return null
    }

    private fun resolveByConventionPath(group: String, name: String, version: String): Package? {
        return results.firstOrNull { result -> result.path == Package.conventionPath(group, name, version) }
    }
}
