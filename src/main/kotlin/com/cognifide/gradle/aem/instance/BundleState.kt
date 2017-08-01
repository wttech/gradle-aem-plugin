package com.cognifide.gradle.aem.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class BundleState private constructor() {

    companion object {
        fun fromJson(json: String): BundleState {
            return ObjectMapper().readValue(json, BundleState::class.java)
        }
    }

    @JsonProperty("data")
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

    val stable: Boolean
        get() = status.endsWith("- all ${bundles.size} bundles active.")

}
