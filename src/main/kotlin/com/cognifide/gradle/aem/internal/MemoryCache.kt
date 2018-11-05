package com.cognifide.gradle.aem.internal

import org.gradle.api.Project

class MemoryCache {

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

        fun of(project: Project): MemoryCache {
            val ext = project.rootProject.extensions.extraProperties
            val key = MemoryCache::class.java.canonicalName
            if (!ext.has(key)) {
                ext.set(key, MemoryCache())
            }

            return ext.get(key) as MemoryCache
        }

    }

}