package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import net.minidev.json.annotate.JsonIgnore
import java.io.Serializable

/**
 * Manages host definitions in case of different purposes.
 *
 * Introduces conventions to distinguish hosts in groups:
 * - 'live' for accessing production site,
 * - 'demo' for accessing site on which automated tests could be performed,
 * - 'author' for accessing AEM author instance,
 * - 'other' for hosts like for AEM dispatcher HTTP server.
 */
class HostOptions(environment: Environment) : Serializable {

    var defined = mutableMapOf<String, MutableList<Host>>()

    @get:JsonIgnore
    val all: List<Host>
        get() = defined.flatMap { it.value }

    @JsonIgnore
    var ipDefault = environment.dockerRuntime.hostIp

    fun define(group: String, name: String, ip: String = ipDefault) {
        defined.getOrPut(group) { mutableListOf() }.add(Host(name, ip))
    }

    fun define(group: String, names: Iterable<String>, ip: String = ipDefault) {
        names.forEach { define(group, it, ip) }
    }

    fun host(group: String): Host = group(group).first()

    fun group(group: String) = defined[group]
            ?: throw EnvironmentException("Environment has no hosts defined in group '$group'")

    // ----- DSL shorthands / conventions -----

    /**
     * Get host responsible for accessing AEM author instance.
     */
    @get:JsonIgnore
    val author: Host
        get() = host(GROUP_AUTHOR)

    /**
     * Get hosts responsible for accessing AEM author instances.
     */
    @get:JsonIgnore
    val authors: List<Host>
        get() = group(GROUP_AUTHOR)

    fun author(name: String) = author(listOf(name))

    fun author(vararg names: String) = author(names.asIterable())

    fun author(names: Iterable<String>) = define(GROUP_AUTHOR, names)

    /**
     * Get host responsible for accessing demo site (e.g used for automated tests)
     */
    @get:JsonIgnore
    val demo: Host
        get() = host(GROUP_DEMO)

    /**
     * Get hosts responsible for accessing demo site (e.g content used for automated tests)
     */
    @get:JsonIgnore
    val demos: List<Host>
        get() = group(GROUP_DEMO)

    fun demo(name: String) = demo(listOf(name))

    fun demo(vararg names: String) = demo(names.asIterable())

    fun demo(names: Iterable<String>) = define(GROUP_DEMO, names)

    /**
     * Get host responsible for accessing live site (production content).
     */
    @get:JsonIgnore
    val live: Host
        get() = host(GROUP_LIVE)

    /**
     * Get hosts responsible for accessing live site (production content).
     */
    @get:JsonIgnore
    val lives: List<Host>
        get() = group(GROUP_LIVE)

    fun live(name: String) = live(listOf(name))

    fun live(vararg names: String) = live(names.asIterable())

    fun live(names: Iterable<String>) = define(GROUP_LIVE, names)

    @get:JsonIgnore
    val others: List<Host>
        get() = group(GROUP_OTHER)

    fun other(name: String) = other(listOf(name))

    fun other(vararg names: String) = other(names.asIterable())

    fun other(names: Iterable<String>) = define(GROUP_OTHER, names)

    companion object {

        val GROUP_AUTHOR = "author"

        val GROUP_DEMO = "demo"

        val GROUP_LIVE = "live"

        val GROUP_OTHER = "other"
    }
}