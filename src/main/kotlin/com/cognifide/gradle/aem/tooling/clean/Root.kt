package com.cognifide.gradle.aem.tooling.clean

import com.cognifide.gradle.aem.common.Patterns
import java.io.File
import java.util.regex.Pattern

object Root {

    private val MANGLE_NAMESPACE_OUT_PATTERN: Pattern = Pattern.compile("/([^:/]+):")

    fun normalize(root: File): File {
        return File(manglePath(Patterns.normalizePath(root.path).substringBefore("/${Cleaner.JCR_CONTENT_NODE}")))
    }

    private fun manglePath(path: String): String {
        var mangledPath = path
        if (path.contains(":")) {
            val matcher = MANGLE_NAMESPACE_OUT_PATTERN.matcher(path)
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
}