package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.ProgressLogger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import org.gradle.api.Project

@Suppress("MagicNumber")
open class ProgressFileDownloader(val project: Project) {

    val progress = ProgressLogger(project)

    var size: Long = 0

    var header: String = "Downloading"

    private var processedBytes: Long = 0

    private var loggedKb: Long = 0

    fun download(input: InputStream, outputFile: File) {
        progress.launch {
            input.use { input ->
                val output = FileOutputStream(outputFile)
                var finished = false

                try {
                    val buf = ByteArray(1024 * 10)
                    var read = input.read(buf)

                    while (read >= 0) {
                        output.write(buf, 0, read)
                        processedBytes += read
                        logProgress(outputFile)
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
            }
        }
    }

    private fun logProgress(outputFile: File) {
        val processedKb = processedBytes / 1024
        if (processedKb > loggedKb) {
            var msg = "Downloading: ${outputFile.name} ${Formats.bytesToHuman(processedBytes)}"
            if (size > 0) {
                msg += "/${Formats.bytesToHuman(size)} [${Formats.percent(processedBytes, size)}]"
            }
            progress.progress(msg)
            loggedKb = processedKb
        }
    }
}