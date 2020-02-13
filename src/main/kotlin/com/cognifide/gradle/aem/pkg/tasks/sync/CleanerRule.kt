package com.cognifide.gradle.aem.pkg.tasks.sync

import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.aem.common.pkg.vault.VaultException
import java.io.File

class CleanerRule(value: String) {

    private var pattern: String = value

    private var excludedPaths: List<String> = listOf()

    private var includedPaths: List<String> = listOf()

    init {
        if (value.contains(PATHS_DELIMITER)) {
            val parts = value.split(PATHS_DELIMITER)
            if (parts.size == 2) {
                pattern = parts[0].trim()
                val paths = parts[1].split(PATH_DELIMITER)
                excludedPaths = paths.filter { it.contains(EXCLUDE_FLAG) }.map { it.removePrefix(EXCLUDE_FLAG).trim() }
                includedPaths = paths.filter { !it.contains(EXCLUDE_FLAG) }.map { it.trim() }
            } else {
                throw VaultException("Cannot parse VLT content property: '$value'")
            }
        }
    }

    private fun isIncluded(file: File) = Patterns.wildcard(file, includedPaths)

    private fun isExcluded(file: File) = Patterns.wildcard(file, excludedPaths)

    fun match(file: File, value: String): Boolean {
        return Patterns.wildcard(value, pattern) && match(file)
    }

    private fun match(file: File): Boolean {
        return (excludedPaths.isEmpty() || !isExcluded(file)) && (includedPaths.isEmpty() || isIncluded(file))
    }

    companion object {
        const val PATHS_DELIMITER = "|"

        const val PATH_DELIMITER = ","

        const val EXCLUDE_FLAG = "!"

        fun manyFrom(props: List<String>): List<CleanerRule> {
            return props.map { CleanerRule(it) }
        }
    }
}
