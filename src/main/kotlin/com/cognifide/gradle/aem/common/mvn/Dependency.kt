package com.cognifide.gradle.aem.common.mvn

data class Dependency(val from: Artifact, val to: Artifact) {

    override fun toString() = "${from.notation} -> ${to.notation}"
}

fun Map.Entry<String, String>.toDependency() = Dependency(this.key.toArtifact(), this.value.toArtifact())

fun Pair<String, String>.toDependency() = Dependency(this.first.toArtifact(), this.second.toArtifact())
