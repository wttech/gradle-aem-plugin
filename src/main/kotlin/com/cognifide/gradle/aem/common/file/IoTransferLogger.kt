package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.ProgressLogger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import org.gradle.api.Project

class IoTransferLogger(val project: Project) : IoTransfer() {

    private val progress = ProgressLogger.of(project)

    private var processedBytes: Long = 0

    private var loggedKb: Long = 0

    override fun upload(file: File, output: OutputStream, cleanup: (File) -> Unit) {
        progress.launch {
            file.inputStream().use { input ->
                var finished = false

                try {
                    val buf = ByteArray(TRANSFER_CHUNK_10_MB)
                    var read = input.read(buf)

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        processedBytes += read
                        logProgress("Uploading", file.length(), file)
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

    override fun download(size: Long, input: InputStream, target: File) {
        progress.launch {
            input.use { inputStream ->
                val output = FileOutputStream(target)
                var finished = false

                try {
                    val buf = ByteArray(TRANSFER_CHUNK_10_MB)
                    var read = inputStream.read(buf)

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        processedBytes += read
                        logProgress("Downloading", size, target)
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

    private fun logProgress(message: String, size: Long, file: File) {
        val processedKb = processedBytes / KILOBYTE
        if (processedKb > loggedKb) {
            val msg = if (size > 0) {
                "$message: ${file.name} | ${Formats.bytesToHuman(processedBytes)}/${Formats.bytesToHuman(size)}"
                        .plus(" [${Formats.percent(processedBytes, size)}]")
            } else {
                "$message: ${file.name} | ${Formats.bytesToHuman(processedBytes)}"
            }

            progress.progress(msg)
            loggedKb = processedKb
        }
    }

    companion object {
        const val KILOBYTE = 1024
    }
}