package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.ProgressLogger
import com.cognifide.gradle.aem.common.utils.Formats
import java.io.File
import java.io.OutputStream

class FileUploader(private val aem: AemExtension) {

    private var processedBytes: Long = 0

    private var loggedKb: Long = 0

    fun ProgressLogger.logProgress(operation: String, readLength: Long, fullLength: Long, file: File) {
        processedBytes += readLength

        val processedKb = processedBytes / KILOBYTE
        if (processedKb > loggedKb) {
            val msg = if (fullLength > 0) {
                "$operation: ${file.name} | ${Formats.bytesToHuman(processedBytes)}/${Formats.bytesToHuman(fullLength)}"
                        .plus(" [${Formats.percent(processedBytes, fullLength)}]")
            } else {
                "$operation: ${file.name} | ${Formats.bytesToHuman(processedBytes)}"
            }

            progress(msg)

            loggedKb = processedKb
        }
    }

    fun upload(file: File, output: OutputStream, cleanup: (File) -> Unit = {}) {
        aem.progressLogger {
            file.inputStream().use { input ->
                var finished = false

                try {
                    val buf = ByteArray(TRANSFER_CHUNK_100_KB)
                    var read = input.read(buf)

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        logProgress("Uploading", read.toLong(), file.length(), file)
                        read = input.read(buf)
                    }

                    output.flush()
                    finished = true
                } finally {
                    output.close()
                    if (!finished) {
                        cleanup(file)
                    }
                }
            }
        }
    }

    companion object {
        const val TRANSFER_CHUNK_100_KB = 100 * 1024

        const val KILOBYTE = 1024
    }
}