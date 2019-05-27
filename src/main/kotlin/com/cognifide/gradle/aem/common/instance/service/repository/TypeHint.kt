package com.cognifide.gradle.aem.common.instance.service.repository

import java.util.*

object TypeHint {

    fun of(value: Any?): String? = when (value) {
        is Iterable<*> -> "${singleValueType(value.firstOrNull())}[]"
        is Array<*> -> "${singleValueType(value.firstOrNull())}[]"
        else -> singleValueType(value)
    }

    private fun singleValueType(value: Any?) = when (value) {
        is String -> "String"
        is Boolean -> "Boolean"
        is Int, Long -> "Long"
        is Date -> "Date"
        is Float -> "Decimal"
        is Double -> "Double"
        else -> null
    }
}