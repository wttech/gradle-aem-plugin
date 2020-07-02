package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationPid {

    @JsonIgnore
    lateinit var instance: Instance

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