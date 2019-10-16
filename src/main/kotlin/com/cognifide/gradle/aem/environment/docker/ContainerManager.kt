package com.cognifide.gradle.aem.environment.docker

class ContainerManager(private val docker: Docker) {

    private val aem = docker.environment.aem

    val defined = mutableListOf<Container>()

    /**
     * Define container.
     */
    fun define(name: String, definition: Container.() -> Unit) {
        defined.add(Container(docker, name).apply(definition))
    }

    /**
     * Shorthand for defining container by string invocation.
     */
    operator fun String.invoke(definition: Container.() -> Unit) = define(this, definition)

    /**
     * Get defined container by name.
     */
    fun named(name: String): Container = defined.firstOrNull { it.name == name }
            ?: throw DockerException("Container named '$name' is not defined!")

    /**
     * Checks if all containers are running.
     */
    val running: Boolean get() = defined.all { it.running }

    fun resolve() {
        aem.progress {
            message = "Resolving container(s): ${defined.names}"
            aem.parallel.each(defined) { it.resolve() }
        }
    }

    fun up() {
        aem.progress {
            message = "Configuring container(s): ${defined.names}"
            aem.parallel.each(defined) { it.up() }
        }
    }

    fun reload() {
        aem.progress {
            message = "Reloading container(s): ${defined.names}"
            aem.parallel.each(defined) { it.reload() }
        }
    }
}
