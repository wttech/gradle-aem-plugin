package com.cognifide.gradle.aem.common.file

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

open class IoTransfer {

    protected open fun logProgress(operation: String, readLength: Long, fullLength: Long, file: File) {}

    open fun upload(file: File, output: OutputStream, cleanup: (File) -> Unit = {}) {
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

    open fun download(size: Long, input: InputStream, target: File) {
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

    companion object {
        const val TRANSFER_CHUNK_100_KB = 100 * 1024
    }
}