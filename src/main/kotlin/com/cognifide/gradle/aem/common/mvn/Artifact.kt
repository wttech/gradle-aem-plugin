package com.cognifide.gradle.aem.common.mvn

data class Artifact(val notation: String) {

    val module get() = notation.substringBeforeLast(":")

    val extension get() = notation.substringAfterLast(":")

    override fun toString() = notation
}

fun String.toArtifact() = Artifact(this)
