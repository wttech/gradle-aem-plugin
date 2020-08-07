package com.cognifide.gradle.aem.common.instance.service.jmx

import com.cognifide.gradle.aem.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class SlingOsgiInstallerState private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    @JsonProperty("Active")
    var active: Boolean = false

    @JsonProperty("SuspendedSince")
    var suspendedSince: Long = 0

    @JsonProperty("ActiveResourceCount")
    var activeResourceCount: Long = 0

    @JsonProperty("InstalledResourceCount")
    var installedResourceCount: Long = -1

    val busy: Boolean get() = active || activeResourceCount > 0

    val unknown get() = installedResourceCount == -1L

    override fun hashCode(): Int = HashCodeBuilder()
            .append(active)
            .append(suspendedSince)
            .append(activeResourceCount)
            .append(installedResourceCount)
            .toHashCode()

    override fun toString(): String = "SlingOsgiInstallerState(instance='${instance.name}', active='$active', activeResourceCount=$activeResourceCount)"

    companion object {
        fun unknown(instance: Instance) = SlingOsgiInstallerState().apply {
            this.instance = instance
        }
    }
}