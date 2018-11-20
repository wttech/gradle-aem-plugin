package com.cognifide.gradle.aem.internal

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Based on: org.gradle.kotlin.dsl.accessors.runtime
 */
class DependencyOptions(private val handler: DependencyHandler) {

    lateinit var group: String

    lateinit var name: String

    var version: String? = null

    var configuration: String? = null

    var classifier: String? = null

    var ext: String? = null

    val dependency: ExternalModuleDependency
        get() = of(handler, group, name, version, configuration, classifier, ext)

    companion object {

        fun of(handler: DependencyHandler, configurer: DependencyOptions.() -> Unit): ExternalModuleDependency {
            return DependencyOptions(handler).apply(configurer).dependency
        }

        fun of(handler: DependencyHandler, group: String, name: String, version: String?, configuration: String?, classifier: String?, ext: String?): ExternalModuleDependency {
            return handler.create(
                    mapOfNonNullValuesOf(
                            "group" to group,
                            "name" to name,
                            "version" to version,
                            "configuration" to configuration,
                            "classifier" to classifier,
                            "ext" to ext
                    )
            ) as ExternalModuleDependency
        }

        private fun mapOfNonNullValuesOf(vararg entries: Pair<String, String?>): Map<String, String> {
            return mutableMapOf<String, String>().apply {
                for ((k, v) in entries) {
                    if (v != null) {
                        put(k, v)
                    }
                }
            }
        }
    }
}