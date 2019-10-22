package com.cognifide.gradle.aem.common.pkg

enum class BundleChecking {
    DISABLED,
    WARN,
    FAIL;

    companion object {
        fun of(name: String): BundleChecking? = values().firstOrNull { it.name.equals(name, true) }
    }
}
