package com.cognifide.gradle.sling.common.pkg

enum class BundleChecking {
    NONE,
    WARN,
    EXCLUDE,
    FAIL;

    companion object {
        fun of(name: String) = values().find { it.name.equals(name, true) }
                ?: throw PackageException("Unsupported bundle checking: $name")
    }
}
