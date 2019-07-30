package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class OsgiConfiguration {

    @JsonProperty
    var pid: String = ""

    @JsonProperty("properties")
    private var configProperties: Map<String, ConfigurationProperty> = mutableMapOf()

    @JsonProperty
    var bundleLocation: String = ""

    @JsonProperty("service_location")
    var serviceLocation: String = ""

    val properties: Map<String, Any> by lazy {
        configProperties.mapValues { it.value.value }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ConfigurationProperty {

        @JsonProperty("value")
        private var singleValue: Any? = null

        @JsonProperty("values")
        private var multiValue: Array<Any>? = null

        val value: Any by lazy {
            singleValue ?: multiValue ?: Any()
        }
    }
}
