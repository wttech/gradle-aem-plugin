package com.cognifide.gradle.aem.common.mvn

import org.gradle.api.tasks.util.PatternFilterable
import java.io.File

class ModuleResolver(val build: MvnBuild) {

    val aem = build.aem

    val all = aem.obj.list<ModuleDescriptor> {
        finalizeValueOnRead()
        set(aem.project.provider {
            when {
                build.available -> aem.project.fileTree(build.rootDir).matching(pomFilter).files.map { pom ->
                    ModuleDescriptor(this@ModuleResolver, typeResolver(pom), pom)
                }
                else -> listOf()
            }
        })
    }

    val pomExclusions = aem.obj.strings {
        set(listOf(
            "**/test/**",
            "**/tests/**",
            "**/*.test/**",
            "**/*-test/**",
            "**/*.tests/**",
            "**/*-tests/**",
            "**/pipeline/**",
            "**/pipelines/**"
        ))
        aem.prop.list("mvnBuild.moduleResolver.pomExclusions")?.let { set(it) }
    }

    val pomFilter: PatternFilterable.() -> Unit = {
        include("pom.xml", "**/pom.xml")
        exclude(build.outputExclusions.get())
        exclude(pomExclusions.get())
    }

    fun byName(name: String) = all.get().firstOrNull { it.name == name }
        ?: throw MvnException("Cannot find module named '$name' in Maven build at path '$rootDir'!")

    fun byArtifact(notation: String) = byArtifact(Artifact(notation))

    fun byArtifact(artifact: Artifact) = all.get().firstOrNull { it.artifactId == artifact.id }
        ?: throw MvnException("Cannot find module for artifact '${artifact.notation}' in Maven build at path '$rootDir'!")

    fun dependency(nameFrom: String, nameTo: String) = Dependency(byName(nameFrom).artifact, byName(nameTo).artifact)

    val rootDir get() = build.rootDir.get().asFile

    val root = all.map { descriptors ->
        descriptors.firstOrNull { it.dir == rootDir }
            ?: throw MvnException("Cannot find root module in Maven build at path '$rootDir'!")
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
        else -> ModuleType.RUN
    }

    fun isRoot(pom: File) = pom.parentFile == rootDir

    fun isPackage(pom: File) = pom.parentFile.resolve(build.contentPath.get()).exists() ||
            pom.readText().contains("<packaging>content-package</packaging>")

    fun isJar(pom: File) = pom.parentFile.resolve("src/main/java").exists() ||
            pom.readText().contains("<packaging>bundle</packaging>")

    fun isFrontend(pom: File) = pom.parentFile.resolve("clientlib.config.js").exists()

    fun isDispatcher(pom: File) = pom.parentFile.resolve("src/conf.dispatcher.d").exists()
}
