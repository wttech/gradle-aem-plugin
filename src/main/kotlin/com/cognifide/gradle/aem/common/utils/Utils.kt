package com.cognifide.gradle.aem.common.utils

import org.apache.commons.lang3.StringUtils
import java.io.File

val Collection<File>.fileNames
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"

fun String.normalizeSeparators(separator: String): String = this.replace(":", separator)
        .replace("-", separator)
        .replace(".", separator)
        .removePrefix(separator)
        .removeSuffix(separator)

@Suppress("MagicNumber")
fun String.shortenClass(maxLength: Int = 32): String {
    val pkgs = split(".").toMutableList()
    var result = this
    if (result.length > maxLength && pkgs.size >= 3) {
        for (i in 1 until (pkgs.size - 1)) {
            pkgs[i] = pkgs[i].first().toString()
            result = pkgs.joinToString(".")
            if (result.length <= maxLength) {
                break
            }
        }
    }
    return StringUtils.abbreviateMiddle(result, "*", maxLength)
}
