package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.utils.Patterns
import java.io.File

/**
 * Allows to reduce logs from analyzing (skip them in incident notifications).
 */
class LogFilter {

    /**
     * Rules defined at build configuration phase.
     */
    val excludeRules = mutableListOf<Log.() -> Boolean>()

    /**
     * Rules that can be added to file (one each line with wildcards) during tailer runtime (without restarting).
     */
    val excludeFiles = mutableListOf<File>()

    fun isExcluded(log: Log): Boolean {
        return excludeRules.any { it(log) } || excludeFiles.any { matchWildcardFile(log, it) }
    }

    fun excludeRule(predicate: Log.() -> Boolean) {
        excludeRules += predicate
    }

    fun excludeFile(file: File) {
        excludeFiles += file
    }

    fun excludeWildcard(vararg matchers: String) = excludeWildcard(matchers.asIterable())

    fun excludeWildcard(matchers: Iterable<String>) = excludeWildcard(matchers.asSequence())

    fun excludeWildcard(matchers: Sequence<String>) = matchers.iterator().forEachRemaining { excludeWildcard(it) }

    fun excludeWildcard(matcher: String) {
        excludeRule { matchWildcard(this, matcher) }
    }

    private fun matchWildcard(log: Log, matcher: String): Boolean {
        return matcher.isNotBlank() && Patterns.wildcard(log.text, "*$matcher*")
    }

    private fun matchWildcardFile(log: Log, file: File): Boolean {
        if (file.exists()) {
            file.useLines { line ->
                line.map { it.trim() }.filter { !it.startsWith(FILE_COMMENT_PREFIX) }.forEach { matcher ->
                    if (matchWildcard(log, matcher)) {
                        return@matchWildcardFile true
                    }
                }
            }
        }

        return false
    }

    companion object {

        const val FILE_COMMENT_PREFIX = "#"
    }
}
