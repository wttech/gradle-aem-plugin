package com.cognifide.gradle.aem.common.pkg.vault

enum class FilterRuleType {
    INCLUDE,
    EXCLUDE;

    companion object {
        fun find(name: String) = values().firstOrNull { it.name.equals(name, true) }

        fun of(name: String) = find(name) ?: throw VaultException("Unsupported Vault filter rule '$name'!")

        fun tags() = values().map { it.name.lowercase() }
    }
}
