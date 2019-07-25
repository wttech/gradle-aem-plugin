package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = ConfigurationDeserializer::class)
@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationState {

    @JsonProperty
    var pid: String = ""

    @JsonProperty
    var properties: List<ConfigurationProperty> = listOf()

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ConfigurationProperty {

        @JsonProperty
        var name: String = ""

        @JsonProperty
        var value: Any = Any()

        @JsonProperty
        var values: List<Any> = listOf()
    }
}
