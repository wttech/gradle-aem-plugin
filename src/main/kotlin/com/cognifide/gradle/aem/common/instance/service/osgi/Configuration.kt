package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class Configuration {

    @JsonProperty
    lateinit var pid: String

    @JsonProperty
    private var title: String? = null

    @JsonProperty
    private var description: String? = null

    @JsonProperty("properties")
    private var rawProperties: Map<String, ConfigurationProperty> = mutableMapOf()

    @JsonProperty
    var bundleLocation: String? = null

    @JsonProperty("service_location")
    var serviceLocation: String? = null

    val properties: Map<String, Any?> by lazy {
        rawProperties.mapValues { it.value.value }
    }

    val valid: Boolean
        get() = !(description?.contains(DESCRIPTION_INVALID, ignoreCase = true) ?: false)

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Configuration

        if (pid != other.pid) return false
        if (rawProperties != other.rawProperties) return false
        if (bundleLocation != other.bundleLocation) return false
        if (serviceLocation != other.serviceLocation) return false

        return true
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(rawProperties)
                .toHashCode()
    }

    override fun toString(): String {
        return "Configuration(pid='$pid', title='$title', description='$description', properties=$properties)"
    }

    companion object {
        const val DESCRIPTION_INVALID = "absence of the OSGi Metatype Service" +
                " or the absence of a MetaType descriptor for this configuration"
    }

}
