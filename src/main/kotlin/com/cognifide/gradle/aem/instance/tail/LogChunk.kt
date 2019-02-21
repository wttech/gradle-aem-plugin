package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.instance.Instance

class LogChunk(val instance: Instance, val logs: List<Log>) {

    val size: Int
        get() = logs.size
}