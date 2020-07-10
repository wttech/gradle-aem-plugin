package com.cognifide.gradle.sling

import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.JavaVersion

class SlingVersion(val value: String) : Comparable<SlingVersion> {

    private val base = Formats.asVersion(value)

    val version get() = base.version

    fun atLeast(other: SlingVersion) = this >= other

    fun atLeast(other: String) = atLeast(SlingVersion(other))

    fun atMost(other: SlingVersion) = other <= this

    fun atMost(other: String) = atMost(SlingVersion(other))

    fun inRange(range: String): Boolean = range.contains("-") && this in unclosedRange(range, "-")

    val frozen get() = true // TODO check it

    // === Overrides ===

    override fun toString() = value

    override fun compareTo(other: SlingVersion): Int = base.compareTo(other.base)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SlingVersion
        if (base != other.base) return false
        return true
    }

    override fun hashCode(): Int = base.hashCode()

    class UnclosedRange(val start: SlingVersion, val end: SlingVersion) {
        operator fun contains(version: SlingVersion) = version >= start && version < end
    }

    companion object {

        val UNKNOWN = SlingVersion("0.0.0")

        val VERSION_6_0_0 = SlingVersion("6.0.0")

        val VERSION_6_1_0 = SlingVersion("6.1.0")

        val VERSION_6_2_0 = SlingVersion("6.2.0")

        val VERSION_6_3_0 = SlingVersion("6.3.0")

        val VERSION_6_4_0 = SlingVersion("6.4.0")

        val VERSION_6_5_0 = SlingVersion("6.5.0")

        fun unclosedRange(value: String, delimiter: String = "-"): UnclosedRange {
            val versions = value.split(delimiter)
            if (versions.size != 2) {
                throw SlingException("Sling version range has invalid format: '$value'!")
            }

            val (start, end) = versions.map { SlingVersion(it) }
            return UnclosedRange(start, end)
        }
    }
}

fun String.javaVersions(delimiter: String = ",") = this.split(delimiter).map { JavaVersion.toVersion(it) }
