package com.cognifide.gradle.aem.common.pkg.validator

import com.cognifide.gradle.aem.common.pkg.PackageException

enum class OakpalSeverity {
    MINOR,
    MAJOR,
    SEVERE;

    companion object {
        fun of(value: String) = values().find { it.name.equals(value, ignoreCase = true) }
            ?: throw PackageException("OakPAL severity named '$value' is not supported!")
    }
}
