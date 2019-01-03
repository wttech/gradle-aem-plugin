package com.cognifide.gradle.aem.common

import com.google.common.collect.Maps

object Collections {

    fun extendMap(first: Map<String, Any>, second: Map<String, Any>): Map<String, Any> {
        return first.toMutableMap().apply { extendMapMutable(this, second) }.toMap()
    }

    @Suppress("unchecked_cast")
    private fun extendMapMutable(first: MutableMap<String, Any>, second: Map<String, Any>) {
        for ((key, value) in second) {
            if (value is Map<*, *>) {
                if (!first.containsKey(key)) {
                    first[key] = Maps.newLinkedHashMap<String, Any>()
                }

                extendMapMutable(first[key] as MutableMap<String, Any>, value as MutableMap<String, Any>)
            } else {
                first[key] = value
            }
        }
    }

    fun mapOfNonNullValues(vararg entries: Pair<String, String?>): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            for ((k, v) in entries) {
                if (v != null) {
                    put(k, v)
                }
            }
        }
    }
}

fun <T> Iterable<T>.onEachApply(block: T.() -> Unit): Iterable<T> {
    return this.onEach { it.apply(block) }
}