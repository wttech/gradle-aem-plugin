package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.common.build.ProgressLogger
import com.cognifide.gradle.aem.common.utils.Formats
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import org.gradle.api.Project

class IoTransferLogger(val project: Project) : IoTransfer() {

    private val progress = ProgressLogger.of(project)

    private var processedBytes: Long = 0

    private var loggedKb: Long = 0

    override fun logProgress(operation: String, readLength: Long, fullLength: Long, file: File) {
        processedBytes += readLength
        val processedKb = processedBytes / KILOBYTE
        if (processedKb > loggedKb) {
            val msg = if (fullLength > 0) {
                "$operation: ${file.name} | ${Formats.bytesToHuman(processedBytes)}/${Formats.bytesToHuman(fullLength)}"
                        .plus(" [${Formats.percent(processedBytes, fullLength)}]")
            } else {
                "$operation: ${file.name} | ${Formats.bytesToHuman(processedBytes)}"
            }
            progress.progress(msg)
            loggedKb = processedKb
        }
    }

    override fun upload(file: File, output: OutputStream, cleanup: (File) -> Unit) {
        progress.launch {
            super.upload(file, output, cleanup)
        }
    }

    override fun download(size: Long, input: InputStream, target: File) {
        progress.launch {
            super.download(size, input, target)
        }
    }

    companion object {
        const val KILOBYTE = 1024
    }
}