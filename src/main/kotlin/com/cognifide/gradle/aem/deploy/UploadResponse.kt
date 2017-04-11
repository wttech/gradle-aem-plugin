package com.cognifide.gradle.aem.deploy

import org.codehaus.jackson.map.ObjectMapper

class UploadResponse private constructor() {

    var isSuccess: Boolean = false

    var msg: String? = null

    var path: String? = null

    companion object {

        fun fromJson(json: String): UploadResponse {
            return ObjectMapper().readValue(json, UploadResponse::class.java)
        }
    }

}
