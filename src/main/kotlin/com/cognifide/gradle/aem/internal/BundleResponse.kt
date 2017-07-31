package com.cognifide.gradle.aem.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class BundleResponse private constructor() {

    companion object {
        fun fromJson(json: String): BundleResponse {
            return ObjectMapper().readValue(json, BundleResponse::class.java)
        }
    }

    lateinit var bundles: List<Bundle>

    lateinit var status: String

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Bundle {
        lateinit var id: String

        lateinit var name: String

        lateinit var stateRaw: String

        val state: Int
            get() = stateRaw.toInt()

        lateinit var symbolicName: String

        lateinit var version: String
    }

}
