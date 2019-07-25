package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class ConfigurationDeserializer : JsonDeserializer<ConfigurationState>() {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): ConfigurationState {
        val result = ConfigurationState()
        val properties = mutableListOf<ConfigurationState.ConfigurationProperty>()
        val root = parser?.codec?.readTree<JsonNode>(parser)
        result.pid = root?.get("pid")?.textValue() ?: ""
        /* root?.get("properties")?.forEach {child ->
        }*/

        return result
    }
}
