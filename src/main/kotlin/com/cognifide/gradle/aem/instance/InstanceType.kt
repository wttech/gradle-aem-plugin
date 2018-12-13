package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.Patterns

enum class InstanceType {
    AUTHOR,
    PUBLISH;

    companion object {

        private val AUTHOR_PORTS = listOf("*02")

        private val AUTHOR_HOST_PREFIXES = listOf("author.", "author-", "autor.", "autor-")

        private val AUTHOR_HOST_SUFFIXES = listOf("-author", "-autor")

        fun byName(type: String): InstanceType {
            return values().find { type.startsWith(it.name, ignoreCase = true) }
                    ?: throw AemException("Invalid instance type: $type")
        }

        fun byUrl(url: String): InstanceType {
            val urlDetails = InstanceUrl.parse(url)

            return when {
                AUTHOR_HOST_PREFIXES.any { urlDetails.config.host.startsWith(it, true) } -> AUTHOR
                AUTHOR_HOST_SUFFIXES.any { urlDetails.config.host.endsWith(it, true) } -> AUTHOR
                Patterns.wildcard(urlDetails.httpPort.toString(), AUTHOR_PORTS) -> AUTHOR
                else -> PUBLISH
            }
        }
    }

    val type: String
        get() = name.toLowerCase()
}