package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Configuration {

    @JsonProperty
    lateinit var pid: String

    @JsonProperty("properties")
    private var configProperties: Map<String, ConfigurationProperty> = mutableMapOf()

    @JsonProperty
    var bundleLocation: String? = null

    @JsonProperty("service_location")
    var serviceLocation: String? = null

    val properties: Map<String, Any?> by lazy {
        configProperties.mapValues { it.value.value }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ConfigurationProperty {

        @JsonProperty("value")
        private var singleValue: Any? = null

        @JsonProperty("values")
        private var multiValue: Array<Any>? = null

        val value: Any? by lazy {
            singleValue ?: multiValue
        }
    }
}
