package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.LogSource
import com.cognifide.gradle.aem.instance.tail.TailOptions
import java.io.BufferedReader

class UrlSource(
    private val options: TailOptions,
    private val instance: Instance
) : LogSource {

    private val aem = options.aem

    private var wasStable = true

    override fun <T> readChunk(parser: (BufferedReader) -> List<T>) =
        handleInstanceUnavailability {
            instance.sync {
                get(options.errorLogEndpoint) {
                    asStream(it).bufferedReader().use(parser)
                }
            }
        }

    private fun <T> handleInstanceUnavailability(parser: () -> List<T>) =
        try {
            val chunk = parser()
            if (!wasStable) {
                aem.logger.info("Tailing resumed for $instance")
                wasStable = true
            }
            chunk
        } catch (ex: RequestException) {
            if (wasStable) {
                aem.logger.warn("Tailing paused for $instance due to '${ex.message}'. Waiting for resuming.")
            }
            wasStable = false
            emptyList<T>()
        }
}