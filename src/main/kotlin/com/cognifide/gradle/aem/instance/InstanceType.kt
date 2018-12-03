package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.Patterns

enum class InstanceType {
    AUTHOR,
    PUBLISH;

    companion object {

        private val AUTHOR_RULES = listOf("*02")

        fun byName(type: String): InstanceType {
            return values().find { type.startsWith(it.name, ignoreCase = true) }
                    ?: throw AemException("Invalid instance type: $type")
        }

        fun nameByUrl(url: String): String {
            return byUrl(url).name.toLowerCase()
        }

        fun byUrl(url: String): InstanceType {
            return byPort(InstanceUrl.parse(url).httpPort)
        }

        fun byPort(port: Int): InstanceType {
            return if (Patterns.wildcard(port.toString(), AUTHOR_RULES)) {
                AUTHOR
            } else {
                PUBLISH
            }
        }
    }

    val type: String
        get() = name.toLowerCase()
}