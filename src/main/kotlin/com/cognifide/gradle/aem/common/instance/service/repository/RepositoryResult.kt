package com.cognifide.gradle.aem.common.instance.service.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RepositoryResult {

    lateinit var title: String

    lateinit var path: String

    @JsonProperty("status.code")
    var statusCode: Int = -1

    lateinit var referer: String

    lateinit var location: String

    lateinit var parentLocation: String

    lateinit var changes: List<RepositoryChange>

    override fun toString(): String {
        return "RepositoryResult(title='$title', path='$path', statusCode=$statusCode)"
    }
}