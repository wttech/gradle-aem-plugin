package com.cognifide.gradle.aem.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    companion object {
        fun fromJson(json: String): ListResponse {
            return ObjectMapper().readValue(json, ListResponse::class.java)
        }
    }

    lateinit var results: List<ListResult>

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ListResult {
        lateinit var pid: String

        lateinit var path: String
    }

}
