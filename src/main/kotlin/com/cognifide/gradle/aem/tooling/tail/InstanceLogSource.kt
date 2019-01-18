package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tasks.Tail
import java.io.BufferedReader
import java.io.InputStreamReader

class InstanceLogSource(private val aemInstance: Instance) : LogSource {

    override fun <T> readChunk(parser: (BufferedReader) -> T) = aemInstance.sync {
        get(ERROR_LOG_ENDPOINT) {
            BufferedReader(InputStreamReader(asStream(it))).use(parser)
        }
    }

    companion object {
        const val ERROR_LOG_ENDPOINT = "/system/console/slinglog/tailer.txt" +
                "?_dc=1520834477194" +
                "&tail=${Tail.NUMBER_OF_LOG_LINES_READ_EACH_TIME}" +
                "&name=%2Flogs%2Ferror.log"
    }
}