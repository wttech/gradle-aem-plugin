package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.pathPrefix
import java.io.File

class ModuleDescriptor(val resolver: ModuleResolver, val type: ModuleType, val pom: File) {

    val build = resolver.build

    val name: String get() = resolver.normalizeArtifactId(artifactId)

    val dir: File get() = pom.parentFile

    val root get() = build.rootDir == dir

    val gav: MvnGav get() = MvnGav.readFile(pom)

    val groupId: String get() = gav.groupId ?: build.groupId.get()

    val artifactId: String get() = gav.artifactId

    val artifact get() = Artifact("$artifactId:${type.extension}")

    val version: String get() = gav.version ?: build.version

    val projectPath get() = "${build.project.pathPrefix}$name"

    val taskPath get() = "${projectPath}:${artifact.extension}"

    override fun toString() = "ModuleDescriptor(name=$name, type=$type)"
}
