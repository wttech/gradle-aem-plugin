package com.cognifide.gradle.sling.common.instance.service.osgi

import com.cognifide.gradle.sling.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationState {

    @JsonIgnore
    lateinit var instance: Instance

    @JsonProperty
    lateinit var pids: List<ConfigurationPid>

    val unknown: Boolean get() = pids.isEmpty()

    override fun toString(): String = "ConfigurationState(instance='${instance.name}', total='${pids.size}')"

    companion object {
        fun unknown(): ConfigurationState = ConfigurationState().apply { pids = listOf() }
    }
}
