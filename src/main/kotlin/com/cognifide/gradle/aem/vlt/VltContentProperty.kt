package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.internal.Patterns
import java.io.File

class VltContentProperty(value: String) {

    private var _name: String = value

    private var _excludePaths: List<String> = listOf()

    init {
        if (value.contains(EXCLUDE_DELIMITER)) {
            val parts = value.split(EXCLUDE_DELIMITER)

            if (parts.size == 2) {
                this._name = parts[0].trim()
                this._excludePaths = parts[1].split(PATH_DELIMITER).map { it.trim() }
            } else {
                throw VltException("Cannot parse VLT content property: '$value'")
            }
        }
    }

    val name: String
        get() = _name

    val excludedPaths: List<String>
        get() = _excludePaths

    fun match(file: File, propValue: String): Boolean {
        return Patterns.wildcard(propValue, name) && match(file)
    }

    fun match(file: File): Boolean {
        return excludedPaths.isEmpty() || !Patterns.wildcard(file, excludedPaths)
    }

    companion object {
        val EXCLUDE_DELIMITER = "!"

        val PATH_DELIMITER = ","

        fun manyFrom(props: List<String>): List<VltContentProperty> {
            return props.map { VltContentProperty(it) }
        }
    }
}