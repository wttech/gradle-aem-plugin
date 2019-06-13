package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.utils.Patterns

enum class IdType {
    AUTHOR,
    PUBLISH;

    companion object {

        private val AUTHOR_PORTS = listOf("*02")

        private val AUTHOR_HOST_PREFIXES = listOf("author.", "author-", "autor.", "autor-")

        private val AUTHOR_HOST_SUFFIXES = listOf("-author", "-autor")

        fun byId(id: String): IdType {
            return values().find { id.startsWith(it.name, ignoreCase = true) }
                    ?: throw AemException("Invalid instance ID '$id'! Must start with prefix 'author' or 'publish'.")
        }

        fun byUrl(url: String): IdType {
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