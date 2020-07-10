package com.cognifide.gradle.sling.common.instance.service.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RepositoryResult {

    lateinit var title: String

    lateinit var path: String

    @JsonProperty("status.code")
    var statusCode: Int = -1

    @JsonProperty("status.message")
    var statusMessage: String? = null

    lateinit var referer: String

    lateinit var location: String

    lateinit var parentLocation: String

    var error: RepositoryError? = null

    lateinit var changes: List<RepositoryChange>

    val success: Boolean
        get() = !fail

    val fail: Boolean
        get() = error != null

    override fun toString(): String {
        return "RepositoryResult(title='$title', path='$path', statusCode=$statusCode, statusMessage=$statusMessage)"
    }
}
