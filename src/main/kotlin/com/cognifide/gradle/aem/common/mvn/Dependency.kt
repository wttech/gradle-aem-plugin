package com.cognifide.gradle.aem.common.mvn

class Dependency(val from: Artifact, val to: Artifact) {

    val notation get() = "${from.notation} ${if (redundant) "~>" else "->"} ${to.notation}"

    var redundant = false

    override fun toString() = "Dependency(from=$from, to=$to, redundant=$redundant)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Dependency
        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }
}

fun Map.Entry<String, String>.toDependency() = Dependency(this.key.toArtifact(), this.value.toArtifact())

fun Pair<String, String>.toDependency() = Dependency(this.first.toArtifact(), this.second.toArtifact())
