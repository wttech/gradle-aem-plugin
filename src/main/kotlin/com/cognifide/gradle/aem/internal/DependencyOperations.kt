package com.cognifide.gradle.aem.internal

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Based on: org.gradle.kotlin.dsl.accessors.runtime
 */
object DependencyOperations {

    fun create(
            dependencyHandler: DependencyHandler,
            group: String,
            name: String,
            version: String?,
            configuration: String?,
            classifier: String?,
            ext: String?
    ): ExternalModuleDependency {
        return dependencyHandler.create(
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