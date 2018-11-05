package com.cognifide.gradle.aem.internal

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import java.io.File

object Patterns {

    fun wildcards(path: String, filters: String): Boolean {
        return filters.split(",").any { filter -> wildcard(path, filter) }
    }

    fun wildcard(file: File, matchers: Iterable<String>): Boolean {
        return matchers.any { wildcard(file, it) }
    }

    fun wildcard(file: File, matcher: String): Boolean {
        return wildcard(file.absolutePath, matcher)
    }

    fun wildcard(path: String, matcher: String): Boolean {
        return wildcard(path, listOf(matcher))
    }

    fun wildcard(path: String, matchers: Iterable<String>): Boolean {
        return matchers.any { matcher -> FilenameUtils.wildcardMatch(normalizePath(path), matcher, IOCase.INSENSITIVE) }
    }

    fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

}