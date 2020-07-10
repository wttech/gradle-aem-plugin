package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingException

enum class PhysicalType {
    LOCAL,
    REMOTE;

    companion object {

        fun byInstance(instance: Instance) = when (instance) {
            is LocalInstance -> LOCAL
            else -> REMOTE
        }

        fun of(type: String?): PhysicalType? {
            if (type.isNullOrBlank()) {
                return null
            }

            return values().find { type.equals(it.name, ignoreCase = true) }
                    ?: throw SlingException("Invalid instance physical type '$type'! Supported values are only: 'local' and 'remote'.")
        }
    }
}
