package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class BundleState private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    @JsonProperty("data")
    lateinit var bundles: List<Bundle>

    lateinit var status: String

    @JsonProperty("s")
    lateinit var stats: List<Int>

    val stable: Boolean get() = !unknown && bundles.all { it.stable }

    val total: Int get() = stats[0]

    val activeBundles: Int get() = stats[1]

    val activeFragments: Int get() = stats[2]

    val resolvedBundles: Int get() = stats[3]

    val installedBundles: Int get() = stats[4]

    val unknown: Boolean get() = bundles.isEmpty()

    val statsWithLabels get() = "${total}t|${activeBundles}ba|${activeFragments}fa|${resolvedBundles}br"

    val stablePercent: String get() = Formats.percentExplained(total - (resolvedBundles + installedBundles), total)

    /**
     * Checks if all bundles of matching symbolic name pattern are stable.
     */
    fun stable(symbolicNames: Iterable<String>): Boolean {
        return !unknown && bundles.filter { Patterns.wildcard(it.symbolicName, symbolicNames) }.all { it.stable }
    }

    fun stable(symbolicName: String): Boolean = stable(listOf(symbolicName))

    fun bundlesExcept(symbolicNames: Iterable<String>): List<Bundle> = bundles.filter { !Patterns.wildcard(it.symbolicName, symbolicNames) }

    /**
     * Checks if all bundles except these matching symbolic name pattern are active.
     */
    fun stableExcept(symbolicNames: Iterable<String>): Boolean = !unknown && bundlesExcept(symbolicNames).all { it.stable }

    fun stableExcept(symbolicName: String): Boolean = stableExcept(listOf(symbolicName))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BundleState

        return EqualsBuilder()
            .append(bundles, other.bundles)
            .append(stats, other.stats)
            .isEquals
    }

    override fun hashCode(): Int = HashCodeBuilder()
        .append(bundles)
        .append(stats)
        .toHashCode()

    override fun toString(): String = "BundleState(instance='${instance.name}', status='$status')"

    companion object {
        fun unknown(e: Exception): BundleState = BundleState().apply {
            bundles = listOf()
            status = e.message ?: "Unknown"
            stats = listOf(0, 0, 0, 0, 0)
        }
    }
}
