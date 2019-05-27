package com.cognifide.gradle.aem.common.instance.service.repository

import java.util.*
import org.apache.jackrabbit.util.ISO8601

object RepositoryType {

    fun hint(value: Any?): String? = when (value) {
        is Iterable<*> -> "${hintSimpleType(value.firstOrNull())}[]"
        is Array<*> -> "${hintSimpleType(value.firstOrNull())}[]"
        else -> hintSimpleType(value)
    }

    private fun hintSimpleType(value: Any?) = when (value) {
        is String -> "String"
        is Boolean -> "Boolean"
        is Int, Long -> "Long"
        is Calendar -> "Date"
        is Date -> "Date"
        is Float -> "Decimal"
        is Double -> "Double"
        else -> null
    }

    fun normalize(value: Any?) = when (value) {
        is Date -> ISO8601.format(Calendar.getInstance().apply { time = value })
        is Calendar -> ISO8601.format(value)
        else -> value
    }
}