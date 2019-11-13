package com.cognifide.gradle.aem.common.pkg.vlt

enum class NodeTypesSync {
    ALWAYS,
    WHEN_AVAILABLE,
    WHEN_MISSING,
    USE_FALLBACK,
    NEVER;

    companion object {

        fun of(name: String) = values().firstOrNull { it.name.equals(name, true) }
    }
}
