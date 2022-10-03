package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.utils.capitalizeChar

enum class ArtifactType {
    POM,
    ZIP,
    JAR,
    RUN;

    val extension get() = name.lowercase()

    val task get() = "mvn${name.capitalizeChar()}"
}
