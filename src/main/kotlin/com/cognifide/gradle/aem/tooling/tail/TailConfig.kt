package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.tooling.tasks.Tail
import org.gradle.api.tasks.Input

class TailConfig {

    @Input
    val filters = mutableListOf<(Log) -> Boolean>()

    @Input
    val blacklistFiles = mutableListOf(Tail.DEFAULT_BLACKLIST_FILE)

    fun blacklistFile(filePath: String) {
        blacklistFiles += filePath
    }

    fun blacklist(filter: (Log) -> Boolean) {
        filters += filter
    }

    fun blacklist(filter: String) {
        filters += { Patterns.wildcard(it.source, filter) || Patterns.wildcard(it.message, filter) }
    }
}