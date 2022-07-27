package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.common.utils.Patterns

/**
 * Represents run mode used to launch AEM instance.
 */
enum class Purpose {

    AUTHOR,
    PUBLISH;

    val type: String get() = name.lowercase()

    val httpUrlDefault: String get() = when (this) {
        AUTHOR -> InstanceUrl.HTTP_AUTHOR_DEFAULT
        PUBLISH -> InstanceUrl.HTTP_PUBLISH_DEFAULT
    }

    val httpPortDefault: Int get() = when (this) {
        AUTHOR -> InstanceUrl.HTTP_AUTHOR_PORT_DEFAULT
        PUBLISH -> InstanceUrl.HTTP_PUBLISH_PORT_DEFAULT
    }

    companion object {

        val AUTHOR_PORTS = listOf("*02")

        val AUTHOR_HOST_PREFIXES = listOf("author.", "author-")

        val AUTHOR_HOST_SUFFIXES = listOf(".author", "-author")

        val PUBLISH_HOST_PREFIXES = listOf("publish.", "publish-")

        val PUBLISH_HOST_SUFFIXES = listOf(".publish", "-publish")

        fun byId(id: String): Purpose {
            return values().find { id.startsWith(it.name, ignoreCase = true) }
                ?: throw AemException("Invalid instance purpose ID '$id'! Must start with prefix 'author' or 'publish'.")
        }

        fun byUrl(url: String): Purpose {
            val urlDetails = InstanceUrl.parse(url)

            return when {
                AUTHOR_HOST_PREFIXES.any { urlDetails.config.host.startsWith(it, true) } -> AUTHOR
                AUTHOR_HOST_SUFFIXES.any { urlDetails.config.host.endsWith(it, true) } -> AUTHOR
                Patterns.wildcard(urlDetails.httpPort.toString(), AUTHOR_PORTS) -> AUTHOR
                else -> PUBLISH
            }
        }

        fun trim(text: String) = text.run {
            var result = this
            AUTHOR_HOST_PREFIXES.forEach { result = result.removePrefix(it) }
            AUTHOR_HOST_SUFFIXES.forEach { result = result.removeSuffix(it) }
            PUBLISH_HOST_PREFIXES.forEach { result = result.removePrefix(it) }
            PUBLISH_HOST_SUFFIXES.forEach { result = result.removeSuffix(it) }
            result
        }
    }
}
