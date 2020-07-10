package com.cognifide.gradle.sling.common.instance.tail

interface LogDestination {
    fun dump(logs: List<Log>)
}
