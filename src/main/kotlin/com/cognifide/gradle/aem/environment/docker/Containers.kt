package com.cognifide.gradle.aem.environment.docker

class Containers(private val docker: Docker) {

    private val aem = docker.environment.aem

    val defined = mutableListOf<Container>()

    fun define(name: String, definition: Container.() -> Unit) {
        defined.add(Container(docker, "${docker.stack.base.name}_$name").apply(definition))
    }

    operator fun String.invoke(definition: Container.() -> Unit) = define(this, definition)

    fun named(name: String): Container = defined.firstOrNull { it.name == name }
            ?: throw DockerException("Container named '$name' is not defined!")

    val running: Boolean get() = defined.all { it.running }

    fun deploy() {
        aem.parallel.each(defined) { it.deploy() }
    }

    fun reload() {
        aem.parallel.map(defined) { it.reload() }
    }
}
