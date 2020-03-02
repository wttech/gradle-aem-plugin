package com.cognifide.gradle.aem.common.instance.tail

interface LogDestination {
    fun dump(logs: List<Log>)
}
