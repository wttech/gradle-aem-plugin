package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.ProgressLogger
import com.cognifide.gradle.aem.common.utils.Formats
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileDownloader(private val aem: AemExtension) {

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

    fun download(size: Long, input: InputStream, target: File) {
        aem.progressLogger {
            input.use { inputStream ->
                val output = FileOutputStream(target)
                var finished = false

                try {
                    val buf = ByteArray(TRANSFER_CHUNK_100_KB)
                    var read = inputStream.read(buf)

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        logProgress("Downloading", read.toLong(), size, target)
                        read = inputStream.read(buf)
                    }

                    output.flush()
                    finished = true
                } finally {
                    output.close()
                    if (!finished) {
                        target.delete()
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