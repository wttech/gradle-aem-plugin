package com.cognifide.gradle.aem.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.ISO8601Utils
import org.apache.commons.validator.routines.UrlValidator
import java.util.*

object Formats {

    val URL_VALIDATOR = UrlValidator(arrayOf("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

    fun toJson(value: Any): String? {
        return ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(value)
    }

    fun toBase64(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray())
    }

    fun bytesToHuman(bytes: Long): String {
        if (bytes < 1024) {
            return bytes.toString() + " B"
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024).toString() + " KB"
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun percent(current: Int, total: Int): String {
        return percent(current.toLong(), total.toLong())
    }

    fun percent(current: Long, total: Long): String {
        val value: Double = if (total == 0L) 0.0 else current.toDouble() / total.toDouble()
        return "${"%.2f".format(value * 100.0)}%"
    }

    fun dateISO8601(date: Date = Date()): String {
        return ISO8601Utils.format(date)
    }

}