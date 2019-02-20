package com.cognifide.gradle.aem.instance.tail

interface LogDestination {
    fun dump(logs: List<Log>)
}
