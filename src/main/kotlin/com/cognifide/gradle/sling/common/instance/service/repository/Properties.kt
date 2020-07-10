package com.cognifide.gradle.sling.common.instance.service.repository

import com.cognifide.gradle.sling.common.utils.JcrUtil
import com.cognifide.gradle.sling.common.instance.service.repository.Node as Base
import com.cognifide.gradle.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import java.text.ParseException
import java.util.*

/**
 * Provides easy conversion of properties to desired types.
 * Sling's ValueMap equivalent.
 */
class Properties(@JsonIgnore val node: Base, props: Map<String, Any>) : LinkedHashMap<String, Any>(props) {

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
        is Array<*> -> {
            if (value.isEmpty()) {
                listOf()
            } else {
                value.map { it.toString() }
            }
        }
        else -> listOf(value.toString())
    }

    fun long(name: String): Long? {
        val string = string(name)
        return try {
            string?.toLong()
        } catch (e: NumberFormatException) {
            throw RepositoryException("Node property '${node.path}[$name]=$string' cannot be converted to long.", e)
        }
    }

    fun longs(name: String): List<Long>? {
        val strings = strings(name)
        return try {
            strings?.map { it.toLong() }
        } catch (e: NumberFormatException) {
            throw RepositoryException("Node property '${node.path}[$name]=$strings' cannot be converted to longs.", e)
        }
    }

    fun double(name: String): Double? {
        val string = string(name)
        return try {
            string?.toDouble()
        } catch (e: NumberFormatException) {
            throw RepositoryException("Node property '${node.path}[$name]=$string' cannot be converted to double.", e)
        }
    }

    fun doubles(name: String): List<Double>? {
        val strings = strings(name)
        return try {
            strings?.map { it.toDouble() }
        } catch (e: NumberFormatException) {
            throw RepositoryException("Node property '${node.path}[$name]=$strings' cannot be converted to doubles.", e)
        }
    }

    fun boolean(name: String): Boolean? = string(name)?.toBoolean()

    fun booleans(name: String): List<Boolean>? = strings(name)?.map { it.toBoolean() }

    fun date(name: String): Date? {
        val string = string(name)
        return try {
            string?.let { RepositoryType.dateFormat().parse(it) }
        } catch (e: ParseException) {
            throw RepositoryException("Node property '${node.path}[$name]=$string' cannot be converted to date.", e)
        }
    }

    fun dates(name: String): List<Date>? {
        val strings = strings(name)
        return try {
            strings?.map { RepositoryType.dateFormat().parse(it) }
        } catch (e: ParseException) {
            throw RepositoryException("Node property '${node.path}[$name]=$strings' cannot be converted to dates.", e)
        }
    }

    fun calendar(name: String): Calendar? = date(name)?.let { JcrUtil.dateToCalendar(it) }

    fun calendars(name: String): List<Calendar>? = dates(name)?.map { JcrUtil.dateToCalendar(it) }

    @get:JsonIgnore
    val json: String get() = Formats.toJson(this)

    // Predefined constants

    val type get() = string("jcr:primaryType")

    val mixinTypes get() = strings("jcr:mixinTypes")

    val title get() = string("jcr:title")

    val description get() = string("jcr:description")

    val created get() = date("jcr:created")

    val createdBy get() = string("jcr:createdBy")

    val lastModified get() = date("jcr:lastModified")

    val lastModifiedBy get() = string("jcr:lastModifiedBy")

    val fileReference get() = string("fileReference")

    val resourceType get() = string("sling:resourceType")

    override fun toString(): String {
        return "Properties(json=${Formats.toJson(this, false)})"
    }
}
