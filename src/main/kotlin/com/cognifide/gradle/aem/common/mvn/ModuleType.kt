package com.cognifide.gradle.aem.common.mvn

enum class ModuleType(val artifact: ArtifactType) {
    POM(ArtifactType.POM),
    JAR(ArtifactType.JAR),
    PACKAGE(ArtifactType.ZIP),
    FRONTEND(ArtifactType.ZIP),
    DISPATCHER(ArtifactType.ZIP),
    RUN(ArtifactType.RUN)
}
