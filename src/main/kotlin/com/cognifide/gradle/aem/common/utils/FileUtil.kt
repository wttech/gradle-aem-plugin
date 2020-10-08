package com.cognifide.gradle.aem.common.utils

import org.apache.commons.io.FilenameUtils

object FileUtil {

    fun sanitizeName(name: String): String = name.replace("[:\\\\/*?|<> &]".toRegex(), "_")

    fun sanitizePath(path: String): String {
        val source = FilenameUtils.separatorsToUnix(path)
        val letter = if (source.contains(":/")) source.substringBefore(":/") else null
        val pathNoLetter = source.substringAfter(":/")
        val pathSanitized = pathNoLetter.split("/").joinToString("/") { sanitizeName(it) }
        return if (letter != null) "$letter:/$pathSanitized" else pathSanitized
    }
}
