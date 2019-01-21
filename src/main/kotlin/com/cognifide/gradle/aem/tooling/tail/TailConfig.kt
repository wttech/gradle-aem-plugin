package com.cognifide.gradle.aem.tooling.tail

import org.gradle.api.tasks.Input

class TailConfig {

    @Input
    val filters = mutableListOf<(Log) -> Boolean>()

    fun blacklist(filter: (Log) -> Boolean) {
        filters += filter
    }
}