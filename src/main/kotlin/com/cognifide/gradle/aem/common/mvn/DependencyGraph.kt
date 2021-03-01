package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.common.pathPrefix

class DependencyGraph(val build: MvnBuild) {

    val aem = build.aem

    val dotFileSource = aem.obj.file {
        set(build.rootDir.file("target/dependency-graph.dot"))
    }

    val dotFile = aem.obj.file {
        set(aem.project.layout.projectDirectory.file("build.mvn.dot"))
    }

    val generateForce = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("mvn.depGraph.generateForce")?.let { set(it) }
    }

    val packagingMap = aem.obj.map<String, String> {
        set(mapOf("content-package" to "zip"))
        aem.prop.map("mvn.depGraph.packagingMap")?.let { set(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun generateDotFile(): String {
        if (generateForce.get() || !dotFile.get().asFile.exists()) {
            try {
                aem.common.progress {
                    step = "Performing full build for dependency graph"
                    aem.project.exec { spec ->
                        spec.workingDir(build.rootDir)
                        spec.executable("mvn")
                        spec.args("clean", "install", "-DskipTests")
                    }

                    step = "Generating dependency graph"
                    aem.project.exec { spec ->
                        spec.workingDir(build.rootDir)
                        spec.executable("mvn")
                        spec.args(
                            "com.github.ferstl:depgraph-maven-plugin:aggregate",
                            "-Dincludes=${build.groupId.get()}",
                            "-Dscope=compile"
                        )
                    }
                    dotFileSource.get().asFile.copyTo(dotFile.get().asFile, true)
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
            }.mapNotNull { (d1, d2) ->
                val d1n = normalizeArtifact(d1)
                val d2n = normalizeArtifact(d2)
                if (d1n != null && d2n != null) Dependency(d1n, d2n) else null
            }.toList()
        })
    }

    val dotArtifacts = aem.obj.list<Artifact> {
        set(aem.obj.provider {
            generateDotFile().lineSequence().mapNotNull { line ->
                line.takeIf { it.contains("[label=") }?.trim()?.substringBefore("[label=")?.removeSurrounding("\"")
            }.mapNotNull { normalizeArtifact(it) }.toList()
        })
    }

    private fun normalizeArtifact(value: String) = value.takeIf { it.endsWith(":compile") }?.removeSuffix(":compile")
        ?.removePrefix("${build.groupId.get()}:")?.removePrefix("${build.appId.get()}.")
        ?.let { dep ->
            packagingMap.get().entries.fold(dep) { depFolded, (packaging, extension) ->
                depFolded.replace(":$packaging", ":$extension")
            }
        }
        ?.let { Artifact(it) }

    val defaults = dotArtifacts.map { da -> da.map { Dependency(it, MvnModule.ARTIFACT_ROOT_POM.toArtifact()) } }

    val extras = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvn.depGraph.extras")?.let { deps -> set(deps.map { it.toDependency() }) }
    }

    fun extras(vararg dependencies: Pair<String, String>) {
        extras.addAll(dependencies.map { it.toDependency() }.asIterable())
    }

    val redundants = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvn.depGraph.redundants")?.let { deps -> set(deps.map { it.toDependency() }) }
    }

    fun redundant(vararg dependencies: Pair<String, String>) {
        redundants.addAll(dependencies.map { it.toDependency() }.asIterable())
    }

    // === Effective values ===

    val all = dotDependencies.map { dd -> (dd + defaults.get() + extras.get()) - redundants.get() }

    val artifacts = dotArtifacts.map { da -> listOf(MvnModule.ARTIFACT_ROOT_POM.toArtifact()) + da }

    val moduleArtifacts = artifacts.map { list ->
        list.fold(mutableMapOf<String, MutableSet<String>>()) { result, artifact ->
            result.also { it.getOrPut(artifact.module) { mutableSetOf() }.add(artifact.extension) }
        }
    }

    val projectPaths = moduleArtifacts.map { ma -> ma.keys.map { "${build.project.pathPrefix}$it" }.sorted() }

    override fun toString() = "DependencyGraph(dotFile=${dotFile.get().asFile}, artifacts=${artifacts.get()}, dependencies=${all.get()})"
}
