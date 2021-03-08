package com.cognifide.gradle.aem.common.mvn

enum class DeployPackageOrder {
    PRECEDENCE,
    GRAPH;

    companion object {
        fun of(value: String) = values().find { value.equals(it.name, ignoreCase = true) }
            ?: throw MvnException("Unsupported build deploy package order '$value' specified for Maven build!")
    }
}
