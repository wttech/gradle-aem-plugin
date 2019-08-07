package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationState {

    @JsonProperty
    lateinit var pids: List<Pid>

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Pid {

        @JsonProperty
        lateinit var id: String

        @JsonProperty
        lateinit var name: String

        @JsonProperty("has_config")
        var hasConfig: Boolean = false

        @JsonProperty
        var fpid: String? = null

        @JsonProperty
        var nameHint: String? = null
    }
}