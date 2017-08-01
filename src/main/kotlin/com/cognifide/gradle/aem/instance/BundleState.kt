package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.internal.Formats
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class BundleState private constructor() {

    companion object {
        fun fromJson(json: String): BundleState {
            return ObjectMapper().readValue(json, BundleState::class.java)
        }

        fun unknown(e: Exception): BundleState {
            val response = BundleState()
            response.bundles = listOf()
            response.status = e.message ?: "Unknown"
            response.counts = listOf(0, 0, 0, 0, 0)

            return response
        }
    }

    @JsonProperty("data")
    lateinit var bundles: List<Bundle>

    lateinit var status: String

    @JsonProperty("s")
    lateinit var counts: List<Int>

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

    val total: Int
        get() = counts[0]

    val activeBundles: Int
        get() = counts[1]

    val activeFragments: Int
        get() = counts[2]

    val resolvedBundles: Int
        get() = counts[3]

    val installedBundles: Int
        get() = counts[4]

    val stablePercent: String
        get() = Formats.percent(total - (resolvedBundles + installedBundles), total)

}
