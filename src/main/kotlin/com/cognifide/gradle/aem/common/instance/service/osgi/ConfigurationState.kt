package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationState {

    @JsonProperty
    var id: String = ""

    @JsonProperty
    var name: String = ""

    @JsonProperty("has_config")
    var hasConfig: Boolean = false

    @JsonProperty
    var fpid: String = ""

    @JsonProperty
    var nameHint: String = ""
}