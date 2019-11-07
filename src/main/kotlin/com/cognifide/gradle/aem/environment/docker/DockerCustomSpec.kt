package com.cognifide.gradle.aem.environment.docker

class DockerCustomSpec(val base: DockerDefaultSpec, val argsOverride: List<String>) : DockerSpec by base {

    override val args: List<String>
        get() = argsOverride
}
