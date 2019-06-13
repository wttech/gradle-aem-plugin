package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException

enum class PhysicalType {
    LOCAL,
    REMOTE;

    companion object {

        fun of(type: String?): PhysicalType? {
            if (type.isNullOrBlank()) {
                return null
            }

            return values().find { type.equals(it.name, ignoreCase = true) }
                    ?: throw AemException("Invalid instance physical type '$type'! Supported values are only: 'local' and 'remote'.")
        }
    }
}