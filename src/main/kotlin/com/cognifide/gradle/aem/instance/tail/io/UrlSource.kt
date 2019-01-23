package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.LogSource
import com.cognifide.gradle.aem.instance.tasks.Tail
import java.io.BufferedReader
import java.io.InputStreamReader

class UrlSource(
    private val aemInstance: Instance,
    private val aem: AemExtension
) : LogSource {

    private var wasStable = true

    override fun <T> readChunk(parser: (BufferedReader) -> List<T>) =
        handleInstanceUnavailability {
            aemInstance.sync {
                get(Tail.ERROR_LOG_ENDPOINT) {
                    BufferedReader(InputStreamReader(asStream(it))).use(parser)
                }
            }
        }

    private fun <T> handleInstanceUnavailability(parser: () -> List<T>) =
        try {
            val chunk = parser()
            if (!wasStable) {
                aem.logger.lifecycle("Restored log tailing from ${aemInstance.httpUrl}")
                wasStable = true
            }
            chunk
        } catch (ex: RequestException) {
            if (wasStable) {
                aem.logger.lifecycle(
                    "Failed to tail logs (${aemInstance.httpUrl}):" +
                        "\n${ex.message}." +
                        "\nWaiting for it to restore."
                )
            }
            wasStable = false
            emptyList<T>()
        }
}