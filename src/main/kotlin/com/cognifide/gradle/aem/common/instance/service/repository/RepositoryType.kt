package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.utils.Formats
import java.text.SimpleDateFormat
import java.util.*
import org.apache.jackrabbit.util.ISO8601

object RepositoryType {

    fun hint(value: Any?): String? = when (value) {
        is Iterable<*> -> "${hintSimpleType(value.firstOrNull())}[]"
        is Array<*> -> "${hintSimpleType(value.firstOrNull())}[]"
        else -> hintSimpleType(value)
    }

    @Suppress("ComplexMethod")
    fun hintSimpleType(value: Any?) = when (value) {
        is String -> "String"
        is Boolean -> "Boolean"
        is Int -> "Long"
        is Long -> "Long"
        is Calendar -> "Date"
        is Date -> "Date"
        is Float -> "Decimal"
        is Double -> "Double"
        else -> null
    }

    fun normalize(value: Any?) = when (value) {
        is Iterable<*> -> value.map { normalizeSimpleType(it) }
        is Array<*> -> value.map { normalizeSimpleType(it) }
        else -> normalizeSimpleType(value)
    }

    fun normalizeSimpleType(value: Any?) = when (value) {
        is Date -> ISO8601.format(Formats.dateToCalendar(value))
        is Calendar -> ISO8601.format(value)
        else -> value
    }

    /**
     * @see <https://github.com/apache/sling-org-apache-sling-servlets-get> - JsonObjectCreator.java
     */
    private const val DATE_FORMAT_ECMA = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z"

    /**
     * @see <https://github.com/apache/sling-org-apache-sling-servlets-get> - JsonObjectCreator.java
     */
    private val DATE_LOCALE = Locale.US

    /**
     * @see <https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#date-properties>
     */
    fun dateFormat() = SimpleDateFormat(DATE_FORMAT_ECMA, DATE_LOCALE)
}
