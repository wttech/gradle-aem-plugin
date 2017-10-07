package com.cognifide.gradle.aem.vlt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException

class VltCleaner(val root: File, val logger: Logger) {

    companion object {
        val VLT_FILE = ".vlt"

        val JCR_CONTENT_FILE = ".content.xml"
    }

    fun removeVltFiles() {
        for (file in FileUtils.listFiles(root, NameFileFilter(VLT_FILE), TrueFileFilter.INSTANCE)) {
            logger.info("Deleting {}", file.path)
            FileUtils.deleteQuietly(file)
        }
    }

    fun cleanupDotContent(contentProperties: List<String>, lineEnding : String) {
        for (file in FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)) {
            try {
                logger.info("Cleaning up {}", file.path)

                val inputLines = FileUtils.readLines(file, CharEncoding.UTF_8)
                val filteredLines = filterLines(inputLines, contentProperties)

                FileUtils.writeLines(file, CharEncoding.UTF_8, filteredLines, lineEnding)
            } catch (e: IOException) {
                throw VltException(String.format("Error opening %s", file.path), e)
            }

        }
    }

    private fun filterLines(lines: List<String>, contentProperties: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (line in lines) {
            val cleanLine = StringUtils.trimToEmpty(line)
            if (contentProperties.any { cleanLine.startsWith(it) }) {
                if (cleanLine.endsWith(">")) {
                    val lastLine = result.removeAt(result.size - 1)
                    result.add(lastLine + ">")
                } else {
                    // skip line with property
                }
            } else {
                result.add(line)
            }

        }

        return result
    }

}

