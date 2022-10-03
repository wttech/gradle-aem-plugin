package com.cognifide.gradle.aem.common.mvn

class Artifact(val notation: String) {

    val id get() = notation.substringBeforeLast(":")

    val type get() = ArtifactType.byExtension(notation.substringAfterLast(":"))

    override fun toString() = "Artifact(id='$id', type='$type')"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Artifact
        if (notation != other.notation) return false
        return true
    }

    override fun hashCode() = notation.hashCode()
}

fun String.toArtifact() = Artifact(this)
