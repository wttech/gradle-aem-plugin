package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.tooling.tasks.Tail
import org.gradle.api.tasks.Input

class TailConfig {

    @Input
    val filters = mutableListOf<(Log) -> Boolean>()

    @Input
    val blacklistFiles = mutableListOf<String>(Tail.DEFAULT_BLACKLIST_FILE)

    fun blacklist(filePath: String) {
        blacklistFiles += filePath
    }

    fun blacklist(filter: (Log) -> Boolean) {
        filters += filter
    }
}