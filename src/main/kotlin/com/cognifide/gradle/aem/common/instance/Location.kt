package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException

/**
 * Indicates if particular instance should be managed by the local instance plugin or not.
 */
enum class Location {

    /**
     * Managed by the plugin which could change it state (make up/down and destroy).
     */
    LOCAL,

    /**
     * Managed externally. Plugin is unable to control it state.
     */
    REMOTE;

    companion object {

        fun byInstance(instance: Instance) = when (instance) {
            is LocalInstance -> LOCAL
            else -> REMOTE
        }

        fun of(type: String?) = find(type)
            ?: throw AemException("Invalid instance physical type '$type'! Supported values are only: 'local' and 'remote'.")

        fun find(type: String?) = values().find { type.equals(it.name, ignoreCase = true) }
    }
}
