package com.cognifide.gradle.aem.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageBuildResponse private constructor() {

    var isSuccess: Boolean = false

    lateinit var msg: String

    lateinit var path: String

    companion object {

        fun fromJson(json: InputStream): PackageBuildResponse {
            return try {
                ObjectMapper().readValue(json, PackageBuildResponse::class.java)
            } catch (e: Exception) {
                throw ResponseException("Malformed package build response.")
            }
        }
    }

}
