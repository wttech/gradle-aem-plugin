package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.internal.http.ResponseException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import org.gradle.api.Project

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    lateinit var results: List<Package>

    fun resolvePackage(project: Project, expected: Package): Package? {
        return PackageResolver.values()
                .asSequence()
                .map { it.resolve(project, this, expected) }
                .firstOrNull { it != null }
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
