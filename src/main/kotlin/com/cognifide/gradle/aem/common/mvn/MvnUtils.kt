package com.cognifide.gradle.aem.common.mvn

import org.gradle.api.tasks.util.PatternFilterable

fun PatternFilterable.excludeTypicalOutputs() = this.exclude(
    // metadata
    "**/.idea/**",
    "**/.idea",
    "**/.gradle/**",
    "**/.gradle",
    "**/gradle.user.properties",
    "**/gradle/user/**",

    // outputs
    "**/target/**",
    "**/target",
    "**/build/**",
    "**/build",
    "**/dist/**",
    "**/dist",
    "**/generated",
    "**/generated/**",

    // temporary files
    "**/node_modules/**",
    "**/node_modules",
    "**/node/**",
    "**/node",
    "**/*.log",
    "**/*.tmp"
)
