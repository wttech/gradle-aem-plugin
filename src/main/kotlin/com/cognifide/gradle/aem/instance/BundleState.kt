package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.internal.Formats
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

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
            response.stats = listOf(0, 0, 0, 0, 0)

            return response
        }
    }

    @JsonProperty("data")
    lateinit var bundles: List<Bundle>

    lateinit var status: String

    @JsonProperty("s")
    lateinit var stats: List<Int>

    val stable: Boolean
        get() = status.endsWith("- all ${bundles.size} bundles active.")

    val total: Int
        get() = stats[0]

    val activeBundles: Int
        get() = stats[1]

    val activeFragments: Int
        get() = stats[2]

    val resolvedBundles: Int
        get() = stats[3]

    val installedBundles: Int
        get() = stats[4]

    val statsWithLabels
        get() = "[$total bt, $activeBundles ba, $activeFragments fa, $resolvedBundles br]"

    val stablePercent: String
        get() = Formats.percent(total - (resolvedBundles + installedBundles), total)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BundleState

        return EqualsBuilder()
                .append(bundles, other.bundles)
                .append(stats, other.stats)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(bundles)
                .append(stats)
                .toHashCode()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Bundle {
        lateinit var id: String

        lateinit var name: String

        lateinit var stateRaw: String

        val state: Int
            get() = stateRaw.toInt()

        lateinit var symbolicName: String

        lateinit var version: String

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Bundle

            return EqualsBuilder()
                    .append(id, other.id)
                    .append(name, other.name)
                    .append(stateRaw, other.stateRaw)
                    .append(symbolicName, other.symbolicName)
                    .append(version, other.version)
                    .isEquals
        }

        override fun hashCode(): Int {
            return HashCodeBuilder()
                    .append(id)
                    .append(name)
                    .append(stateRaw)
                    .append(symbolicName)
                    .append(version)
                    .toHashCode()
        }
    }

}
