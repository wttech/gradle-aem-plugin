package com.cognifide.gradle.aem.common.mvn

enum class ModuleType(val extension: String) {
    POM(Artifact.POM),
    JAR(Artifact.JAR),
    PACKAGE(Artifact.ZIP),
    FRONTEND(Artifact.ZIP),
    DISPATCHER(Artifact.ZIP),
    OTHER(Artifact.OTHER)
}
