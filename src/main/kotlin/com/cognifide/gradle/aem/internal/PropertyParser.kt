package com.cognifide.gradle.aem.internal

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.gradle.api.Project

class PropertyParser(val project : Project) {

    companion object {
        val FILTER_DEFAULT = "*"
    }

    fun filter(value: String, propName : String, propDefault : String = FILTER_DEFAULT) : Boolean {
        val filters = project.properties.getOrElse(propName, { propDefault }) as String

        return filters.split(",").any { group ->
            FilenameUtils.wildcardMatch(value, group, IOCase.INSENSITIVE)
        }
    }

}
