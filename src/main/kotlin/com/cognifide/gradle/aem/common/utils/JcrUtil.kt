package com.cognifide.gradle.aem.common.utils

import java.util.*
import java.util.regex.Pattern

object JcrUtil {

    fun date(date: Date = Date()): String {
        return "" // TODO handle ISO8601 convertion without jackrabbit dependency
        // return ISO8601.format(Calendar.getInstance().apply { time = date })
    }
    fun dateToCalendar(date: Date): Calendar {
        return Calendar.getInstance().apply { time = date }
    }

    /**
     * Converts e.g ':jcr:content' to '_jcr_content' (part of JCR path to be valid OS path).
     */
    fun manglePath(path: String): String = when {
        !path.contains("/") -> manglePathInternal("/$path").removePrefix("/")
        else -> manglePathInternal(path)
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
