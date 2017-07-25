package com.cognifide.gradle.aem.internal

import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileDownloader(val project: Project, url: String, val size: Long = 0) {

    val progressLogger = ProgressLogger(project, "Downloading file: $url")

    private var processedBytes: Long = 0

    private var loggedKb: Long = 0

    fun download(input: InputStream, outputFile: File) {
        try {
            progressLogger.started()

            val output = FileOutputStream(outputFile)
            var finished = false

            try {
                val buf = ByteArray(1024 * 10)
                var read = input.read(buf)

                while (read >= 0) {
                    output.write(buf, 0, read)
                    processedBytes += read
                    logProgress()
                    read = input.read(buf)
                }

                output.flush()
                finished = true
            } finally {
                output.close()
                if (!finished) {
                    outputFile.delete()
                }
            }
        } finally {
            input.close()
            progressLogger.completed()
        }
    }

    private fun logProgress() {
        val processedKb = processedBytes / 1024
        if (processedKb > loggedKb) {
            var msg = Formats.bytesToHuman(processedBytes)
            if (size > 0) {
                msg += "/" + Formats.bytesToHuman(size)
            }
            msg += " downloaded"
            progressLogger.progress(msg)
            loggedKb = processedKb
        }
    }

}