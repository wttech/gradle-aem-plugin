package com.cognifide.gradle.aem.environment.docker

// TODO progress logger on dev task not visible
class Containers(private val docker: Docker) {

    private val aem = docker.environment.aem

    val defined = mutableListOf<Container>()

    fun define(name: String, definition: Container.() -> Unit) {
        defined.add(Container(docker, name).apply(definition))
    }

    operator fun String.invoke(definition: Container.() -> Unit) = define(this, definition)

    fun named(name: String): Container = defined.firstOrNull { it.name == name }
            ?: throw DockerException("Container named '$name' is not defined!")

    val running: Boolean get() = defined.all { it.running }

    fun resolve() {
        aem.progress {
            message = "Resolving containers"
            aem.parallel.each(defined) { it.resolve() }
        }
    }

    fun up() {
        aem.progress {
            message = "Turning on containers"
            aem.parallel.each(defined) { it.up() }
        }
    }

    fun reload() {
        aem.progress {
            message = "Reloading containers"
            aem.parallel.each(defined) { it.reload() }
        }
    }
}
