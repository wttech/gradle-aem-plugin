package com.cognifide.gradle.aem.common.build

import org.gradle.api.Project

class BuildScope {

    private val cache = mutableMapOf<String, Any>()

    @Suppress("unchecked_cast")
    fun <T : Any> get(key: String): T? {
        return cache[key] as T
    }

    @Suppress("unchecked_cast")
    fun <T : Any> getOrPut(key: String, defaultValue: () -> T, invalidate: Boolean = false): T {
        return if (invalidate) {
            val value = defaultValue()
            put(key, value)
            value
        } else {
            cache.getOrPut(key, defaultValue) as T
        }
    }

    fun <T : Any> put(key: String, value: T) {
        cache[key] = value
    }

    companion object {

        fun of(project: Project): BuildScope {
            val ext = project.rootProject.extensions.extraProperties
            val key = BuildScope::class.java.canonicalName
            if (!ext.has(key)) {
                ext.set(key, BuildScope())
            }

            return ext.get(key) as BuildScope
        }
    }
}