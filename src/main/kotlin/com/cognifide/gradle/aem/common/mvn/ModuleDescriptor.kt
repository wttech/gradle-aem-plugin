package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.pathPrefix
import com.cognifide.gradle.common.utils.Formats
import java.io.File

class ModuleDescriptor(val resolver: ModuleResolver, val type: ModuleType, val pom: File) {

    val build = resolver.build

    val name: String get() = Formats.relativePath(dir.absolutePath, build.rootDir.get().asFile.absolutePath)
        .replace("\\", "/").replace("/", ":")
        .ifBlank { NAME_ROOT }

    val dir: File get() = pom.parentFile

    val root get() = build.rootDir.get().asFile == dir

    val gav: MvnGav get() = MvnGav.readFile(pom)

    val groupId: String get() = gav.groupId ?: build.groupId.get()

    val artifactId: String get() = gav.artifactId

    val artifactTaskPath get() = taskPath(artifact.extension)

    val artifact get() = Artifact("$artifactId:${type.extension}")

    val version: String get() = gav.version ?: build.version

    val projectPath get() = "${build.project.pathPrefix}$name"

    fun taskPath(name: String) = "$projectPath:$name"

    fun hasProfile(name: String) = pom.readText()
        .substringAfter("<profiles>").substringBefore("</profiles>")
        .contains("<id>${name.removePrefix("!")}</id>")

    fun determineOsgiConfigPath(appId: String): File {
        val paths = listOf(
            "/apps/$appId/osgiconfig/config",
            "/apps/$appId/config",
        ).map { path -> dir.resolve("${build.contentPath.get()}/jcr_root/${path.removePrefix("/")}") }
        return paths.firstOrNull { it.exists() }
            ?: throw MvnException((listOf(
                "Cannot find existing OSGi config path for Maven module! Searched paths:"
            ) + paths.map { it.toString() }).joinToString("\n"))
    }

    override fun toString() = "ModuleDescriptor(name=$name, type=$type)"

    companion object {
        const val NAME_ROOT = "root"
    }
}
