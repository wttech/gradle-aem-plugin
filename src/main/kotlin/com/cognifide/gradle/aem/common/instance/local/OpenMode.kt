package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.LocalInstanceException

/**
 * Controls when browser will be opened when instance is up.
 */
enum class OpenMode {

    /**
     * Only once when instance is initialized (up first time).
     */
    ONCE,

    /**
     * Always when instance is up (also after restarting).
     */
    ALWAYS,

    /**
     * Browser will never be opened when instance is up.
     */
    NEVER;

    companion object {
        fun of(name: String): OpenMode {
            return values().find { it.name.equals(name, true) }
                ?: throw LocalInstanceException("Unsupported local instance open mode named: $name")
        }
    }
}
