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

    @Suppress("TooGenericExceptionCaught")
    private fun generateDotFile(): String {
        if (generateForce.get() || !dotFile.get().asFile.exists()) {
            try {
                aem.project.exec { spec ->
                    spec.workingDir(build.rootDir)
                    spec.executable("mvn")
                    spec.args("com.github.ferstl:depgraph-maven-plugin:aggregate", "-Dincludes=${build.groupId.get()}")
                }
            } catch (e: Exception) {
                throw MvnException("Cannot generate Maven DepGraph properly! Error: '${e.message}'", e)
            }
        }

        val file = dotFile.get().asFile
        if (!file.exists()) {
            throw MvnException("Maven DepGraph file does not exist: '$file'!")
        }

        return file.readText()
    }

    val dotDependencies = aem.obj.list<Dependency> {
        set(aem.obj.provider {
            generateDotFile().lineSequence().mapNotNull { line ->
                line.takeIf { it.contains(" -> ") }?.trim()?.split(" -> ")?.let {
                    it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"")
                }
            }.map { normalizeArtifact(it.first) to normalizeArtifact(it.second) }.toList()
        })
    }

    val dotArtifacts = aem.obj.list<String> {
        set(aem.obj.provider {
            generateDotFile().lineSequence().mapNotNull { line ->
                line.takeIf { it.contains("[label=") }?.trim()?.substringBefore("[label=")?.removeSurrounding("\"")
            }.map { normalizeArtifact(it) }.toList()
        })
    }

    private fun normalizeArtifact(value: String) = value
        .removePrefix("${build.groupId.get()}:")
        .removePrefix("${build.appId.get()}.")
        .replace(":content-package:compile", ":zip")
        .replace(":jar:compile", ":jar")
        .replace(":zip:compile", ":zip")

    val defaults = dotArtifacts.map { da -> da.map { MvnModule.ARTIFACT_ROOT_POM to it } }

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

    // === Effective values ===

    val all = dotDependencies.map { dd -> (dd + defaults.get() + extras.get()) - redundants.get() }

    val artifacts = dotArtifacts.map { da -> listOf(MvnModule.ARTIFACT_ROOT_POM) + da }

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
