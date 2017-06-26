package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.fasterxml.jackson.databind.util.ISO8601Utils
import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*

class PropertyParser(val project: Project) {

    companion object {
        val FILTER_DEFAULT = "*"
    }

    fun filter(value: String, propName: String, propDefault: String = FILTER_DEFAULT): Boolean {
        val filters = project.properties.getOrElse(propName, { propDefault }) as String

        return filters.split(",").any { group -> Patterns.wildcard(value, group) }
    }

    fun expand(source: String, properties: Map<String, Any> = mapOf()): String {
        try {
            val interpolated = StrSubstitutor.replace(source, systemProperties)
            val allProperties = aemProperties + properties
            val template = SimpleTemplateEngine().createTemplate(interpolated).make(allProperties)

            return template.toString()
        } catch (e: Throwable) {
            throw AemException("Cannot expand properly all properties. Probably used non-existing field name. Source: '$source'", e)
        }
    }

    val systemProperties: Map<String, String> by lazy {
        System.getProperties().entries.fold(mutableMapOf<String, String>(), { props, prop ->
            props.put(prop.key.toString(), prop.value.toString()); props
        })
    }

    val aemProperties: Map<String, Any>
        get() {
            val config = AemConfig.of(project)
            val buildDate = Date()

            return mapOf(
                    "rootProject" to project.rootProject,
                    "project" to project,
                    "config" to config,
                    "instances" to config.instancesByName,
                    "created" to ISO8601Utils.format(buildDate),
                    "buildCount" to SimpleDateFormat("yDDmmssSSS").format(buildDate)
            )
        }

}
