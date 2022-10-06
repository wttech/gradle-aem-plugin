package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.utils.capitalizeChar

enum class ArtifactType {
    POM,
    ZIP,
    JAR,
    RUN;

    val extension get() = name.lowercase()

    val task get() = "mvn${extension.capitalizeChar()}"

    companion object {
        fun byExtension(value: String) = values().firstOrNull { it.extension == value }
            ?: error("Artifact type cannot be determined by extension '$value'!")
    }
}
