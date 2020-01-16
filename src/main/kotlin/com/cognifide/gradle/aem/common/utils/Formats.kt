package com.cognifide.gradle.aem.common.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
import java.util.regex.Pattern

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

    fun jsonMapper() = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    fun jsonWriter(pretty: Boolean) = jsonMapper().run {
        when {
            pretty -> writer(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            })
            else -> writer()
        }
    }

    fun asJson(input: InputStream) = JsonPath.parse(input)

    fun asJson(value: String) = JsonPath.parse(value)

    fun toJson(value: Any, pretty: Boolean = true): String {
        return jsonWriter(pretty).writeValueAsString(value) ?: ""
    }

    fun toJson(value: Map<String, Any?>, pretty: Boolean = true): String {
        return jsonWriter(pretty).writeValueAsString(value) ?: "{}"
    }

    inline fun <reified T : Any> fromJson(json: String) = fromJson(json, T::class.java)

    fun <T> fromJson(json: String, clazz: Class<T>): T = jsonMapper().readValue(json, clazz)

    fun fromJsonToMap(json: String): Map<String, Any?> = jsonMapper().run {
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

    fun duration(millis: Long): String = DurationFormatUtils.formatDuration(millis.coerceAtLeast(0L), "mm:ss")

    fun durationSince(millis: Long) = duration(System.currentTimeMillis() - millis)

    fun durationFit(thenMillis: Long, thenZoneId: ZoneId, durationMillis: Long): Boolean {
        val nowTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val thenTimestamp = dateTime(thenMillis, thenZoneId)
        val diffMillis = ChronoUnit.MILLIS.between(thenTimestamp, nowTimestamp)

        return diffMillis < durationMillis
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

    /**
     * Splits command to arguments usually delimited by space
     * while considering quoted string containing spaces as single argument.
     */
    fun commandToArgs(command: String): List<String> {
        val quotedSpaceToken = "@@@SPACE@@@"
        var tokenizedCommand = command

        Regex("'([^']+)'").findAll(command).iterator().forEachRemaining {
            val quotedString = it.groupValues[1]
            val tokenizedString = quotedString.replace(" ", quotedSpaceToken)
            tokenizedCommand = tokenizedCommand.replace("'$quotedString'", tokenizedString)
        }

        return StringUtils.split(tokenizedCommand, " ").map { it.replace(quotedSpaceToken, " ") }
    }

    /**
     * Converts e.g ':jcr:content' to '_jcr_content' (part of JCR path to be valid OS path).
     */
    fun manglePath(path: String): String = when {
        !path.contains("/") -> manglePathInternal("/$path").removePrefix("/")
        else -> manglePathInternal(path)
    }

    fun camelToSeparated(text: String, separator: String = "_"): String {
        return text.replace("(.)(\\p{Upper})".toRegex(), "$1$separator$2").toLowerCase()
    }

    private fun manglePathInternal(path: String): String {
        var mangledPath = path
        if (path.contains(":")) {
            val matcher = MANGLE_NAMESPACE_PATTERN.matcher(path)
            val buffer = StringBuffer()
            while (matcher.find()) {
                val namespace = matcher.group(1)
                matcher.appendReplacement(buffer, "/_${namespace}_")
            }
            matcher.appendTail(buffer)
            mangledPath = buffer.toString()
        }
        return mangledPath
    }

    private val MANGLE_NAMESPACE_PATTERN: Pattern = Pattern.compile("/([^:/]+):")
}

val Collection<File>.fileNames
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
