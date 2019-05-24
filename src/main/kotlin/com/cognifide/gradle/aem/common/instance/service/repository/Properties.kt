package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import org.apache.jackrabbit.util.ISO8601

class Properties(props: Map<String, Any>) : LinkedHashMap<String, Any>(props) {

    fun string(name: String): String? = get(name)?.toString()

    fun strings(name: String): List<String>? = when (val value = get(name)) {
        null -> null
        is List<*> -> {
            if (value.isEmpty()) {
                listOf()
            } else {
                value.map { it.toString() }
            }
        }
        else -> listOf()
    }

    fun long(name: String): Long? = string(name)?.toLong()

    fun longs(name: String): List<Long>? = strings(name)?.map { it.toLong() }

    fun double(name: String): Double? = string(name)?.toDouble()

    fun doubles(name: String): List<Double>? = strings(name)?.map { it.toDouble() }

    fun boolean(name: String): Boolean? = string(name)?.toBoolean()

    fun booleans(name: String): List<Boolean>? = strings(name)?.map { it.toBoolean() }

    fun calendar(name: String): Calendar? = string(name)?.let { ISO8601.parse(it) }

    fun calendars(name: String): List<Calendar>? = strings(name)?.map { ISO8601.parse(it) }

    fun date(name: String): Date? = calendar(name)?.time

    fun dates(name: String): List<Date>? = strings(name)?.map { ISO8601.parse(it).time }

    @get:JsonIgnore
    val json: String
        get() = Formats.toJson(this)

    override fun toString(): String {
        return "Properties(json=${Formats.toJson(this, false)})"
    }
}