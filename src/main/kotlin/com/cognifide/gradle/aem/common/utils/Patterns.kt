package com.cognifide.gradle.aem.common.utils

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase

object Patterns {

    const val WILDCARD_NEGATION = "!"

    const val WILDCARD_SEPARATOR = ","

    fun wildcard(file: File, matcher: String): Boolean {
        return wildcard(file.absolutePath, matcher)
    }

    fun wildcard(file: File, matchers: Iterable<String>): Boolean {
        return wildcard(file.absolutePath, matchers)
    }

    fun wildcard(path: String, matcher: String): Boolean {
        return wildcard(path, if (matcher.contains(WILDCARD_SEPARATOR)) {
            matcher.split(WILDCARD_SEPARATOR)
        } else {
            listOf(matcher)
        })
    }

    fun wildcard(path: String, matchers: Iterable<String>): Boolean {
        val includes = matchers.filter { !it.startsWith(WILDCARD_NEGATION) }
        val excludes = matchers.filter { it.startsWith(WILDCARD_NEGATION) }.map { it.removePrefix(WILDCARD_NEGATION) }

        return wildcardMatch(path, includes, excludes)
    }

    private fun wildcardMatch(path: String, includes: Iterable<String>, excludes: Iterable<String>): Boolean {
        return includes.any { wildcardMatch(path, it) } && excludes.none { wildcardMatch(path, it) }
    }

    private fun wildcardMatch(path: String, matcher: String): Boolean {
        return FilenameUtils.wildcardMatch(Formats.normalizePath(path), matcher, IOCase.INSENSITIVE)
    }
}
