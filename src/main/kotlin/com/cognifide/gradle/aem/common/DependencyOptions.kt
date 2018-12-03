package com.cognifide.gradle.aem.common

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Based on: org.gradle.kotlin.dsl.accessors.runtime
 */
@Suppress("LongParameterList")
class DependencyOptions(private val handler: DependencyHandler) {

    lateinit var group: String

    lateinit var name: String

    var version: String? = null

    var configuration: String? = null

    var classifier: String? = null

    var ext: String? = null

    fun dependency(
        group: String,
        name: String,
        version: String? = null,
        configuration: String? = null,
        classifier: String? = null,
        ext: String? = null
    ) {
        this.group = group
        this.name = name
        this.version = version
        this.configuration = configuration
        this.classifier = classifier
        this.ext = ext
    }

    val dependency: ExternalModuleDependency
        get() = handler.create(Collections.mapOfNonNullValues(
                "group" to group,
                "name" to name,
                "version" to version,
                "configuration" to configuration,
                "classifier" to classifier,
                "ext" to ext
        )) as ExternalModuleDependency

    companion object {

        fun of(handler: DependencyHandler, configurer: DependencyOptions.() -> Unit): ExternalModuleDependency {
            return DependencyOptions(handler).apply(configurer).dependency
        }
    }
}