package com.cognifide.gradle.aem.common.mvn

class DependencyGraph(val build: MvnBuild) {

    val aem = build.aem

    val dotFileSource = aem.obj.file {
        set(build.rootDir.file("target/dependency-graph.dot"))
    }

    val dotFile = aem.obj.file {
        set(aem.project.layout.projectDirectory.file("build.maven.dot"))
    }

    val generateForce = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("mvnBuild.depGraph.generateForce")?.let { set(it) }
    }

    val generateCommand = aem.obj.string {
        convention(build.groupId.map { "com.github.ferstl:depgraph-maven-plugin:aggregate -Dincludes=${build.groupId.get()} -Dscope=compile" })
        aem.prop.string("mvnBuild.depGraph.generateCommand")?.let { set(it) }
    }

    val buildCommand = aem.obj.string {
        convention("clean install -DskipTests")
        aem.prop.string("mvnBuild.depGraph.buildCommand")?.let { set(it) }
    }

    val packagingMap = aem.obj.map<String, String> {
        set(mapOf("content-package" to "zip"))
        aem.prop.map("mvnBuild.depGraph.packagingMap")?.let { set(it) }
    }

    val artifactSuffixes = aem.obj.strings {
        convention(listOf(":compile", ":provided"))
        aem.prop.list("mvnBuild.depGraph.artifactSuffixes")?.let { set(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun generateDotFile(): String {
        if (generateForce.get() || !dotFile.get().asFile.exists()) {
            val buildDir = build.rootDir.get().asFile
            if (!buildDir.exists()) {
                throw MvnException("Maven build root directory does not exist: '$buildDir'!")
            }

            try {
                aem.common.progress {
                    step = "Performing Maven build for dependency graph"
                    aem.common.mvn {
                        workingDir(buildDir)
                        args(buildCommand.get().split(" "))
                    }

                    step = "Generating Maven build dependency graph"
                    aem.common.mvn {
                        workingDir(buildDir)
                        args(generateCommand.get().split(" "))
                    }
                    dotFileSource.get().asFile.copyTo(dotFile.get().asFile, true)
                }
            } catch (e: Exception) {
                throw MvnException("Cannot generate Maven DepGraph properly!", e)
            }
        }

        val file = dotFile.get().asFile
        if (!file.exists()) {
            throw MvnException("Maven DepGraph file does not exist: '$file'!")
        }

        return file.readText()
    }

    val dotDependencies = aem.obj.list<Dependency> {
        finalizeValueOnRead()
        set(
            aem.obj.provider {
                generateDotFile().lineSequence().mapNotNull { line ->
                    line.takeIf { it.contains(" -> ") }?.trim()?.split(" -> ")?.let {
                        it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"")
                    }
                }.mapNotNull { (d1, d2) ->
                    val d1n = normalizeArtifact(d1)
                    val d2n = normalizeArtifact(d2)
                    if (d1n != null && d2n != null) Dependency(d1n, d2n) else null
                }.toList()
            }
        )
    }

    val dotArtifacts = aem.obj.list<Artifact> {
        finalizeValueOnRead()
        set(
            aem.obj.provider {
                generateDotFile().lineSequence().mapNotNull { line ->
                    line.takeIf { it.contains("[label=") }?.trim()?.substringBefore("[label=")?.removeSurrounding("\"")
                }.mapNotNull { normalizeArtifact(it) }.toList()
            }
        )
    }

    private fun normalizeArtifact(value: String) = value
        .takeIf { v -> artifactSuffixes.get().any { v.endsWith(it) } }
        ?.let { v -> artifactSuffixes.get().fold(v) { r, s -> r.removeSuffix(s) } }
        ?.removePrefix("${build.groupId.get()}:")
        ?.let { dep ->
            packagingMap.get().entries.fold(dep) { depFolded, (packaging, extension) ->
                depFolded.replace(":$packaging", ":$extension")
            }
        }
        ?.let { Artifact(it) }

    val defaults = dotArtifacts.map { da ->
        da.flatMap { a ->
            build.moduleResolver.all.get()
                .filter { m -> m.type == ModuleType.POM }
                .map { m -> Dependency(a, m.artifact) }
        }
    }

    val extras = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvnBuild.depGraph.extras")?.let { deps -> set(deps.map { it.toDependency() }) }
    }

    fun extra(vararg dependencies: Pair<String, String>) {
        extras.addAll(dependencies.map { it.toDependency() }.asIterable())
    }

    fun extraModule(vararg names: Pair<String, String>) {
        extras.addAll(namesToDependencies(names.asIterable()))
    }

    val redundants = aem.obj.list<Dependency> {
        convention(listOf())
        aem.prop.map("mvnBuild.depGraph.redundants")?.let { deps -> set(deps.map { it.toDependency() }) }
    }

    fun redundant(vararg dependencies: Pair<String, String>) {
        redundants.addAll(dependencies.map { it.toDependency() }.asIterable())
    }

    fun redundantModule(vararg names: Pair<String, String>) {
        redundants.addAll(namesToDependencies(names.asIterable()))
    }

    val softRedundants = aem.obj.list<Dependency> {
        aem.prop.map("mvnBuild.depGraph.softRedundants")?.let { deps -> set(deps.map { it.toDependency() }) }
    }

    fun softRedundant(vararg dependencies: Pair<String, String>) {
        softRedundants.addAll(dependencies.map { it.toDependency() }.asIterable())
    }

    fun softRedundantModule(vararg names: Pair<String, String>) {
        softRedundants.addAll(namesToDependencies(names.asIterable()))
    }

    val all = dotDependencies
        .map { dd -> (dd + defaults.get() + extras.get()) - redundants.get() } // combine all
        .map { da -> da.filter { it.from.id != it.to.id } } // remove cycles
        .map { dd -> // mark redundants
            dd.onEach { dep ->
                if (softRedundants.get().contains(dep)) {
                    val artifactId = dep.to.id
                    val module = build.modules.get().firstOrNull { it.descriptor.artifactId == artifactId }
                        ?: throw MvnException("Cannot find module with artifactId '$artifactId' to determine soft redundant dependency '$dep'!")
                    if (module.repositoryPom.get().asFile.exists()) {
                        dep.redundant = true
                    }
                }
            }
        }

    private fun namesToDependencies(names: Iterable<Pair<String, String>>) = aem.project.provider {
        names.map { (from, to) -> build.moduleResolver.dependency(from, to) }
    }

    override fun toString() = "DependencyGraph(dotFile=${dotFile.get().asFile}, dependencies=${all.get().map { it.notation }})"
}
