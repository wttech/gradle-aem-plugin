package com.cognifide.gradle.sling.common.instance.service.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RepositoryError {

    @JsonProperty("class")
    lateinit var className: String

    lateinit var message: String

    override fun toString(): String {
        return "RepositoryError(class='$className', message='$message')"
    }
}
