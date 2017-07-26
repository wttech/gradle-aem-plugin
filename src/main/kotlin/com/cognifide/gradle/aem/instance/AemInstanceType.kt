package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.internal.Patterns

enum class AemInstanceType {
    AUTHOR,
    PUBLISH;

    companion object {

        val authorRules = listOf("*02")

        fun byName(type: String): AemInstanceType {
            return values().find { it.name.equals(type, true) } ?: throw AemException("Invalid instance type: $type")
        }

        fun nameByUrl(url: String): String {
            return byUrl(url).name.toLowerCase()
        }

        fun byUrl(url: String): AemInstanceType {
            return byPort(AemInstance.portOfUrl(url))
        }

        fun byPort(port: Int): AemInstanceType {
            return if (Patterns.wildcard(port.toString(), authorRules)) {
                AUTHOR
            } else {
                PUBLISH
            }
        }
    }

}