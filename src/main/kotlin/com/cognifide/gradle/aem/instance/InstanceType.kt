package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemException
import com.cognifide.gradle.aem.internal.Patterns

enum class InstanceType {
    AUTHOR,
    PUBLISH;

    companion object {

        val authorRules = listOf("*02")

        fun byName(type: String): InstanceType {
            return values().find { it.name.equals(type, true) } ?: throw AemException("Invalid instance type: $type")
        }

        fun nameByUrl(url: String): String {
            return byUrl(url).name.toLowerCase()
        }

        fun byUrl(url: String): InstanceType {
            return byPort(Instance.portOfUrl(url))
        }

        fun byPort(port: Int): InstanceType {
            return if (Patterns.wildcard(port.toString(), authorRules)) {
                AUTHOR
            } else {
                PUBLISH
            }
        }
    }

}