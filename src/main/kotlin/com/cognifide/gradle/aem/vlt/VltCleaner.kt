package com.cognifide.gradle.aem.vlt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.util.*

class VltCleaner(val root: String, val logger: Logger) {

    companion object {
        val VLT_FILE = ".vlt"

        val JCR_CONTENT_FILE = ".content.xml"
    }

    fun removeVltFiles() {
        for (file in FileUtils.listFiles(File(root), NameFileFilter(VLT_FILE), TrueFileFilter.INSTANCE)) {
            logger.info("Deleting {}", file.path)
            FileUtils.deleteQuietly(file)
        }
    }

    fun cleanupDotContent(contentProperties: List<String>) {
        for (file in FileUtils.listFiles(File(root), NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)) {
            try {
                logger.info("Cleaning up {}", file.path)

                val lines = ArrayList<String>()
                for (line in FileUtils.readLines(file, CharEncoding.UTF_8)) {
                    val cleanLine = StringUtils.trimToEmpty(line)
                    val lineContains = lineContainsProperty(cleanLine, contentProperties)
                    if (lineContains) {
                        if (!cleanLine.endsWith(">")) {
                        } else {
                            val lastLine = lines.removeAt(lines.size - 1)
                            lines.add(lastLine + ">")
                        }

                    } else {
                        lines.add(line)
                    }

                }

                FileUtils.writeLines(file, CharEncoding.UTF_8, lines)
            } catch (e: IOException) {
                throw VltException(String.format("Error opening %s", file.path), e)
            }

        }
    }

    private fun lineContainsProperty(cleanLine: String, contentProperties: List<String>): Boolean {
        return contentProperties.any { cleanLine.startsWith(it) }
    }

}

