package com.cognifide.gradle.aem.common.mvn

import java.io.File

class ModuleResolver(val build: MvnBuild) {

    val aem = build.aem

    val all = aem.obj.list<ModuleDescriptor> {
        finalizeValueOnRead()
        set(aem.project.provider {
            aem.project.fileTree(build.rootDir)
                .matching { it.include("pom.xml", "**/pom.xml").exclude(build.outputPatterns.get()) }
                .files.map { pom -> ModuleDescriptor(this@ModuleResolver, typeResolver(pom), pom) }
        })
    }

    fun byName(name: String) = all.get().firstOrNull { it.name == name }
        ?: throw MvnException("Cannot find module named '$name' in Maven build at path '$rootDir'!")

    fun byArtifact(artifact: Artifact) = all.get().firstOrNull { it.artifact == artifact }
        ?: throw MvnException("Cannot find module for artifact '${artifact.notation}' in Maven build at path '$rootDir'!")

    val rootDir get() = build.rootDir.get().asFile

    val root = all.map { descriptors ->
        descriptors.firstOrNull { it.dir == rootDir }
            ?: throw MvnException("Cannot find root module in Maven build at path '$rootDir'!")
    }

    val artifactPrefixes = aem.obj.strings {
        set(aem.project.provider {
            listOf(
                "${build.groupId.get()}:",
                "${build.appId.get()}."
            )
        })
    }

    fun normalizeArtifactId(id: String): String {
        val prefixes = artifactPrefixes.get().toMutableList()

        val separators = listOf(".", "-")
        separators.forEach { separator ->
            val rootPrefix = "${root.get().artifactId}${separator}"
            val rootPrefixedOthers = all.get().filter { !it.root }.all { it.artifactId.startsWith(rootPrefix) }
            if (rootPrefixedOthers) {
                prefixes.add(rootPrefix)
            }
        }

        return when (id) {
            root.get().artifactId -> ROOT_DESCRIPTOR_NAME
            else -> prefixes.fold(id) { r, n -> r.removePrefix(n) }
        }
    }

    val projectPaths = all.map { descriptors -> descriptors.map { it.projectPath }.sorted() }

    fun typeResolver(resolver: (File) -> ModuleType) {
        this.typeResolver = resolver
    }

    private var typeResolver: (File) -> ModuleType = { pom -> resolveType(pom) }

    fun resolveType(pom: File) = when {
        isJar(pom) -> ModuleType.JAR
        isPackage(pom) -> ModuleType.PACKAGE
        isFrontend(pom) -> ModuleType.FRONTEND
        isDispatcher(pom) -> ModuleType.DISPATCHER
        isRoot(pom) -> ModuleType.POM
        else -> ModuleType.OTHER
    }

    fun isRoot(pom: File) = pom.parentFile == rootDir

    fun isPackage(pom: File) = pom.parentFile.resolve("src/main/content").exists() ||
            pom.readText().contains("<packaging>content-package</packaging>")

    fun isJar(pom: File) = pom.parentFile.resolve("src/main/java").exists()

    fun isFrontend(pom: File) = pom.parentFile.resolve("clientlib.config.js").exists()

    fun isDispatcher(pom: File) = pom.parentFile.resolve("src/conf.dispatcher.d").exists()

    companion object {
        const val ROOT_DESCRIPTOR_NAME = "root"
    }
}
