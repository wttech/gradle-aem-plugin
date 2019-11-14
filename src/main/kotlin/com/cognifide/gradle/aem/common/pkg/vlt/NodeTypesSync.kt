package com.cognifide.gradle.aem.common.pkg.vlt

enum class NodeTypesSync {
    ALWAYS,
    AUTO,
    PRESERVE_AUTO,
    FALLBACK,
    PRESERVE_FALLBACK,
    NEVER;

    companion object {

        fun of(name: String) = values().firstOrNull { it.name.equals(name, true) }
    }
}
