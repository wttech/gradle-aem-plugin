package com.cognifide.gradle.aem.common.utils

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.File
import java.nio.charset.StandardCharsets

object FileUtil {

    fun sanitizeName(name: String): String = name.replace("[:\\\\/*?|<> &]".toRegex(), "_")

    fun sanitizePath(path: String): String {
        val source = FilenameUtils.separatorsToUnix(path)
        val letter = if (source.contains(":/")) source.substringBefore(":/") else null
        val pathNoLetter = source.substringAfter(":/")
        val pathSanitized = pathNoLetter.split("/").joinToString("/") { sanitizeName(it) }
        return if (letter != null) "$letter:/$pathSanitized" else pathSanitized
    }

    fun systemPath(path: String) = FilenameUtils.separatorsToSystem(path)

    fun readLastLines(file: File, count: Int): List<String> {
        return ReversedLinesFileReader(file, StandardCharsets.UTF_8).readLines(count)
    }
}
