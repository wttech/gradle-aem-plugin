package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.pathPrefix

typealias Dependency = Pair<String, String>

class DependencyGraph(val build: MvnBuild) {

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

    private fun parse(): List<Dependency> {
        val dotContents = dotFile.get().asFile.readText()
        return dotContents.lineSequence().mapNotNull { line ->
            line.takeIf { it.contains(" -> ") }?.trim()?.split(" -> ")?.let {
                it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"")
            }
        }.toList()
    }

    private fun build(): List<Dependency> {
        generate()
        return parse()
    }

    val origins = aem.obj.list<Dependency> {
        set(aem.obj.provider {
            build().map { (d1, d2) -> normalizeArtifact(d1) to normalizeArtifact(d2) }
        })
    }

    private fun normalizeArtifact(value: String) = value
        .removePrefix("${build.groupId.get()}:")
        .removePrefix("${build.appId.get()}.")
        .replace(":content-package:compile", ":zip")
        .replace(":jar:compile", ":jar")
        .replace(":zip:compile", ":zip")

    val defaults = aem.obj.list<Dependency> {
        set(origins.map { deps ->
            val unique = deps.flatMap { listOf(it.first, it.second) }.toSet()
            unique.map { "${MvnModule.NAME_ROOT}:${MvnModule.ARTIFACT_POM}" to it }
        })
        aem.prop.map("mvn.depGraph.defaults")?.let { deps -> set(deps.map { it.toPair() }) }
    }

    val extras = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvn.depGraph.extras")?.let { deps -> set(deps.map { it.toPair() }) }
    }

    fun extras(vararg dependencies: Pair<String, String>) {
        extras.addAll(dependencies.asIterable())
    }

    val redundants = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvn.depGraph.redundants")?.let { deps -> set(deps.map { it.toPair() }) }
    }

    fun redundant(vararg dependencies: Pair<String, String>) {
        redundants.addAll(dependencies.asIterable())
    }

    val all = origins.map { deps ->
        (deps + defaults.get() + extras.get()) - redundants.get()
    }

    val artifacts = all.map { deps -> deps.flatMap { listOf(it.first, it.second) }.toSet() }

    val moduleArtifacts = artifacts.map { deps ->
        deps.fold(mutableMapOf<String, MutableSet<String>>()) { result, dep ->
            val name = dep.substringBefore(":")
            val extension = dep.substringAfter(":")
            result.getOrPut(name) { mutableSetOf() }.add(extension)
            result
        }
    }

    val projectPaths = moduleArtifacts.map { ma -> ma.keys.map { "${build.project.pathPrefix}$it" }.sorted() }
}
