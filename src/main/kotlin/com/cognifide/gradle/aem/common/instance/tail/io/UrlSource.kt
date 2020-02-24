package com.cognifide.gradle.aem.common.instance.tail.io

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.tail.Tailer
import com.cognifide.gradle.aem.common.instance.tail.LogSource
import com.cognifide.gradle.common.http.RequestException
import java.io.BufferedReader

class UrlSource(
    private val tailer: Tailer,
    private val instance: Instance
) : LogSource {

    private val logger = tailer.aem.logger

    private var wasStable = true

    override fun <T> readChunk(parser: (BufferedReader) -> List<T>) =
        handleInstanceUnavailability {
            instance.sync.http {
                get(tailer.errorLogEndpoint(instance)) {
                    asStream(it).bufferedReader().use(parser)
                }
            }
        }

    private fun <T> handleInstanceUnavailability(parser: () -> List<T>) =
        try {
            val chunk = parser()
            if (!wasStable) {
                logger.lifecycle("Tailing resumed for $instance")
                wasStable = true
            }
            chunk
        } catch (ex: RequestException) {
            if (wasStable) {
                logger.warn("Tailing paused for $instance due to '${ex.message}'. Awaiting resumption.")
            }
            wasStable = false
            emptyList<T>()
        }
}
