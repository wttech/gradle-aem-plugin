package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

/**
 * Manages host definitions in case of different purposes indicated by tags.
 */
class HostOptions(environment: Environment) : Serializable {

    var defined = mutableListOf<Host>()

    @JsonIgnore
    var ipDefault = environment.dockerRuntime.hostIp

    fun define(url: String, options: Host.() -> Unit = {}) {
        defined.add(Host(url).apply { ip = ipDefault; options() })
    }

    fun define(vararg urls: String, options: Host.() -> Unit = {}) = define(urls.asIterable(), options)

    fun define(urls: Iterable<String>, options: Host.() -> Unit = {}) = urls.forEach { define(it, options) }

    fun find(vararg tags: String) = find(tags.asIterable())

    fun find(tags: Iterable<String>) = all(tags).first()

    fun all(vararg tags: String) = all(tags.asIterable())

    fun all(tags: Iterable<String>) = defined.filter { h -> tags.all { t -> h.tags.contains(t) } }.ifEmpty {
        throw EnvironmentException("Environment has no hosts tagged with '$tags'!")
    }

    // ----- DSL shorthands / conventions -----

    /**
     * Get host responsible for accessing AEM author instance.
     */
    @get:JsonIgnore
    val author: Host
        get() = find(TAG_AUTHOR)

    /**
     * Get hosts responsible for accessing AEM author instances.
     */
    @get:JsonIgnore
    val authors: List<Host>
        get() = all(TAG_AUTHOR)

    fun author(vararg urls: String, options: Host.() -> Unit = {}) = author(urls.asIterable(), options)

    fun author(urls: Iterable<String>, options: Host.() -> Unit = {}) = define(urls) { tag(TAG_AUTHOR); options() }

    /**
     * Get host responsible for accessing AEM author instance.
     */
    @get:JsonIgnore
    val publish: Host
        get() = find(TAG_PUBLISH)

    /**
     * Get hosts responsible for accessing AEM publish instances.
     */
    @get:JsonIgnore
    val publishes: List<Host>
        get() = all(TAG_PUBLISH)

    fun publish(vararg urls: String, options: Host.() -> Unit = {}) = publish(urls.asIterable(), options)

    fun publish(urls: Iterable<String>, options: Host.() -> Unit = {}) = define(urls) { tag(TAG_PUBLISH); options() }

    @get:JsonIgnore
    val others: List<Host>
        get() = all(TAG_OTHER)

    fun other(vararg urls: String, options: Host.() -> Unit = {}) = other(urls.asIterable(), options)

    fun other(urls: Iterable<String>, options: Host.() -> Unit = {}) = define(urls) { tag(TAG_OTHER); options() }

    companion object {

        val TAG_AUTHOR = "author"

        val TAG_PUBLISH = "publish"

        val TAG_OTHER = "other"
    }
}
