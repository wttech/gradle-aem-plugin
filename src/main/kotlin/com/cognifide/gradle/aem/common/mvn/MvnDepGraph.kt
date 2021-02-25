package com.cognifide.gradle.aem.common.mvn

typealias MvnDependency = Pair<String, String>

class MvnDepGraph(val build: MvnBuild) {

    val aem = build.aem

    val dotFile = aem.obj.file {
        set(build.rootDir.file("target/dependency-graph.dot"))
    }

    val generateForce = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("mvn.depGraph.generateForce")?.let { set(it) }
    }

    private fun generate() {
        if (!generateForce.get() && dotFile.get().asFile.exists()) {
            return
        }
        aem.project.exec { spec ->
            spec.workingDir(build.rootDir)
            spec.executable("mvn")
            spec.args("com.github.ferstl:depgraph-maven-plugin:aggregate", "-Dincludes=${build.groupId}")
        }
    }

    private fun parse(): List<MvnDependency> {
        val dotContents = dotFile.get().asFile.readText()
        return dotContents.lineSequence().mapNotNull { line ->
            line.takeIf { it.contains(" -> ") }?.trim()?.split(" -> ")?.let {
                it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"")
            }
        }.toList()
    }

    private fun build(): List<MvnDependency> {
        generate()
        return parse()
    }

    val dependencies = aem.obj.list<MvnDependency> {
        set(aem.obj.provider { build() })
    }

    val artifactDependencies = dependencies.map { deps ->
        val result = deps.map { (d1, d2) -> normalizeArtifact(d1) to normalizeArtifact(d2) }
        val unique = result.flatMap { listOf(it.first, it.second) }.toSet()
        result + unique.map { "${MvnModule.NAME_ROOT}:${MvnModule.ARTIFACT_POM}" to it }
    }

    private fun normalizeArtifact(value: String) = value
        .removePrefix("${build.groupId.get()}:")
        .removePrefix("${build.appId.get()}.")
        .replace(":content-package:compile", ":zip")
        .replace(":jar:compile", ":jar")
        .replace(":zip:compile", ":zip")

    val artifacts = artifactDependencies.map { deps -> deps.flatMap { listOf(it.first, it.second) }.toSet() }

    val moduleArtifacts = artifacts.map { deps ->
        deps.fold(mutableMapOf<String, MutableSet<String>>()) { result, dep ->
            val name = dep.substringBefore(":")
            val extension = dep.substringAfter(":")
            result.getOrPut(name) { mutableSetOf() }.add(extension)
            result
        }
    }

    val projectPaths = moduleArtifacts.map { ma -> ma.keys.map { "${build.projectPathPrefix}$it" }.sorted() }
}
