package com.cognifide.gradle.aem.deploy

import org.codehaus.jackson.map.ObjectMapper

class ListResponse private constructor() {

    companion object {
        fun fromJson(json: String): ListResponse {
            return ObjectMapper().readValue(json, ListResponse::class.java)
        }
    }

    var isSuccess: Boolean = false

    var results: List<Result>? = null


    class Result {
        var pid: String? = null

        val path: String? = null
    }

}
