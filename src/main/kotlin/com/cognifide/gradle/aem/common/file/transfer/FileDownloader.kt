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

    fun ProgressLogger.logProgress(operation: String, readLength: Long, fullLength: Long, file: File, nanoStartTime: Long) {
        processedBytes += readLength

        val nanoElapsedTime = System.currentTimeMillis() - nanoStartTime
        val allDownloadTime = (nanoElapsedTime * fullLength / processedBytes)
        val nanoRemainingTime = allDownloadTime - nanoElapsedTime

        val processedKb = processedBytes / KILOBYTE
        if (processedKb > loggedKb) {
            val fileName = file.name.removeSuffix(FileTransferManager.TMP_SUFFIX)
            val msg = if (fullLength > 0) {
                "$operation: $fileName | ${Formats.bytesToHuman(processedBytes)}/${Formats.bytesToHuman(fullLength)}"
                        .plus(" (${Formats.percent(processedBytes, fullLength)})")
                        .plus(" time left: ${Formats.durationFormatted(nanoRemainingTime)}")
            } else {
                "$operation: $fileName | ${Formats.bytesToHuman(processedBytes)}"
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
                    val nanoStartTime = System.currentTimeMillis()

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        logProgress("Downloading", read.toLong(), size, target, nanoStartTime)
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