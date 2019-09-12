package com.cognifide.gradle.aem.common.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.commons.validator.routines.UrlValidator
import org.apache.jackrabbit.util.ISO8601
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Suppress("MagicNumber", "TooManyFunctions")
object Formats {

    val URL_VALIDATOR = UrlValidator(arrayOf("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

    fun asPassword(value: String) = "*".repeat(value.length)

    /**
     * Trims e.g ".SP2" in "6.1.0.SP2" which is not valid Gradle version
     */
    fun asVersion(value: String): GradleVersion {
        if (value.isBlank()) {
            return VERSION_UNKNOWN
        }

        return try {
            GradleVersion.version(value.split(".").take(3).joinToString("."))
        } catch (e: IllegalArgumentException) {
            return VERSION_UNKNOWN
        }
    }

    fun versionAtLeast(actual: String, required: String) = asVersion(actual) >= asVersion(required)

    val VERSION_UNKNOWN = GradleVersion.version("0.0.0")

    fun jsonMapper(pretty: Boolean): ObjectMapper = ObjectMapper().apply {
        if (pretty) {
            writer(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            })
        }
    }

    fun asJson(input: InputStream) = JsonPath.parse(input)

    fun asJson(value: String) = JsonPath.parse(value)

    fun toJson(value: Any, pretty: Boolean = true): String {
        return jsonMapper(pretty).writeValueAsString(value) ?: ""
    }

    fun toJson(value: Map<String, Any?>, pretty: Boolean = true): String {
        return jsonMapper(pretty).writeValueAsString(value) ?: "{}"
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return ObjectMapper().readValue(json, clazz)
    }

    fun fromJsonToMap(json: String): Map<String, Any?> = ObjectMapper().run {
        readValue(json, typeFactory.constructMapType(HashMap::class.java, String::class.java, Any::class.java))
    }

    fun toList(value: String?, delimiter: String = ","): List<String>? {
        if (value.isNullOrBlank()) {
            return null
        }

        val between = StringUtils.substringBetween(value, "[", "]") ?: value
        if (between.isBlank()) {
            return null
        }

        return between.split(delimiter)
    }

    fun toMap(value: String?, valueDelimiter: String = ",", keyDelimiter: String = "="): Map<String, String>? {
        return toList(value, valueDelimiter)?.map { v ->
            v.split(keyDelimiter).let { e ->
                when (e.size) {
                    2 -> e[0] to e[1]
                    else -> v to ""
                }
            }
        }?.toMap()
    }

    fun toBase64(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray())
    }

    fun size(file: File): String {
        return bytesToHuman(when {
            file.exists() -> FileUtils.sizeOf(file)
            else -> 0L
        })
    }

    fun bytesToHuman(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> (bytes / 1024).toString() + " KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun percent(current: Int, total: Int): String {
        return percent(current.toLong(), total.toLong())
    }

    fun percentExplained(current: Int, total: Int): String {
        return "$current/$total=${percent(current, total)}"
    }

    fun percent(current: Long, total: Long): String {
        val value: Double = when (total) {
            0L -> 0.0
            else -> current.toDouble() / total.toDouble()
        }

        return "${"%.2f".format(value * 100.0)}%"
    }

    fun percentExplained(current: Long, total: Long) = "$current/$total=${percent(current, total)}"

    fun noLineBreaks(text: String): String {
        return text.replace("\r\n", " ").replace("\n", " ")
    }

    fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }

    fun date(date: Date = Date()): String {
        return ISO8601.format(Calendar.getInstance().apply { time = date })
    }

    fun dateToCalendar(date: Date): Calendar {
        return Calendar.getInstance().apply { time = date }
    }

    fun dateTime(timestamp: Long, zoneId: ZoneId): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
    }

    fun dateFileName(date: Date = Date()): String {
        return SimpleDateFormat("yyyyMMddHHmmss").format(date)
    }

    fun timeUp(thenMillis: Long, thenZoneId: ZoneId, durationMillis: Long): Boolean {
        val nowTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val thenTimestamp = dateTime(thenMillis, thenZoneId)
        val diffMillis = ChronoUnit.MILLIS.between(thenTimestamp, nowTimestamp)

        return diffMillis < durationMillis
    }

    fun duration(millis: Long): String {
        return DurationFormatUtils.formatDurationHMS(millis)
    }

    fun durationFormatted(millis: Long): String {
        return DurationFormatUtils.formatDuration(millis, "mm:ss:SSS")
    }

    fun rootProjectPath(file: File, project: Project): String {
        return rootProjectPath(file.absolutePath, project)
    }

    fun rootProjectPath(path: String, project: Project): String {
        return projectPath(path, project.rootProject)
    }

    fun projectPath(file: File, project: Project): String {
        return projectPath(file.absolutePath, project)
    }

    fun projectPath(path: String, project: Project): String {
        return relativePath(path, project.projectDir.absolutePath)
    }

    fun relativePath(path: String, basePath: String): String {
        val source = Paths.get(path)
        val base = Paths.get(basePath)

        return base.relativize(source).toString()
    }

    fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

    fun normalizeSeparators(name: String, separator: String): String {
        return name.replace(":", separator)
                .replace("-", separator)
                .replace(".", separator)
                .removePrefix(separator)
                .removeSuffix(separator)
    }

    fun calculateChecksum(text: String): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        val data = text.toByteArray()
        messageDigest.update(data, 0, data.size)
        val result = BigInteger(1, messageDigest.digest())
        return String.format("%1$032x", result)
    }
}

val Collection<File>.fileNames
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
