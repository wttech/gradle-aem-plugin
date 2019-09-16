package com.cognifide.gradle.aem.common.utils

object Utils {

    fun mapOfNonNullValues(vararg entries: Pair<String, String?>): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            for ((k, v) in entries) {
                if (v != null) {
                    put(k, v)
                }
            }
        }
    }

    fun unroll(value: Any?, callback: (Any?) -> Unit) = when (value) {
        is Array<*> -> value.forEach { callback(it) }
        is Iterable<*> -> value.forEach { callback(it) }
        else -> callback(value)
    }
}

fun <T> Iterable<T>.onEachApply(block: T.() -> Unit): Iterable<T> {
    return this.onEach { it.apply(block) }
}
