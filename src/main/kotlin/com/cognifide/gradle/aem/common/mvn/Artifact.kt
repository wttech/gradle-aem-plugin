package com.cognifide.gradle.aem.common.mvn

class Artifact(val notation: String) {

    val id get() = notation.substringBeforeLast(":")

    val extension get() = notation.substringAfterLast(":")

    override fun toString() = "Artifact(id='$id', extension='$extension')"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Artifact
        if (notation != other.notation) return false
        return true
    }

    override fun hashCode() = notation.hashCode()

    companion object {

        const val POM = "pom"

        const val ZIP = "zip"

        const val JAR = "jar"

        const val RUN = "run"
    }
}

fun String.toArtifact() = Artifact(this)
