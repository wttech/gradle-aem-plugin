package com.cognifide.gradle.aem.tooling.tail.io

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tail.LogSource
import com.cognifide.gradle.aem.tooling.tasks.Tail
import java.io.BufferedReader
import java.io.InputStreamReader

class UrlSource(private val aemInstance: Instance) : LogSource {

    override fun <T> readChunk(parser: (BufferedReader) -> T) = aemInstance.sync {
        get(Tail.ERROR_LOG_ENDPOINT) {
            BufferedReader(InputStreamReader(asStream(it))).use(parser)
        }
    }
}