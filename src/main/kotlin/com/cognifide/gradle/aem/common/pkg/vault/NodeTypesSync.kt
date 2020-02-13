package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.common.pkg.PackageException

enum class NodeTypesSync {
    ALWAYS,
    AUTO,
    PRESERVE_AUTO,
    FALLBACK,
    PRESERVE_FALLBACK,
    NEVER;

    companion object {

        fun find(name: String) = values().firstOrNull { it.name.equals(name, true) }

        fun of(name: String) = find(name)
                ?: throw PackageException("Unsupported package node types sync mode '$name'!")
    }
}
