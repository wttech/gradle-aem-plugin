package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.AemConfig
import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project

class PropertyParser(val project: Project) {

    companion object {
        val FILTER_DEFAULT = "*"
    }

    private val config = AemConfig.of(project)

    fun filter(value: String, propName: String, propDefault: String = FILTER_DEFAULT): Boolean {
        val filters = project.properties.getOrElse(propName, { propDefault }) as String

        return filters.split(",").any { group ->
            FilenameUtils.wildcardMatch(value, group, IOCase.INSENSITIVE)
        }
    }

    fun expand(source: String, predefinedProperties: Map<String, Any>): String {
        val interpolated = StrSubstitutor.replace(source, systemProperties)
        val templateProps = predefinedProperties + config.vaultExpandProperties
        val template = SimpleTemplateEngine().createTemplate(interpolated).make(templateProps)

        return template.toString()
    }

    val systemProperties: Map<String, String>
        get() {
            return System.getProperties().entries.fold(mutableMapOf<String, String>(), { props, prop ->
                props.put(prop.key.toString(), prop.value.toString()); props
            })
        }

}
