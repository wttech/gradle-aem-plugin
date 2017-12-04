package com.cognifide.gradle.aem.vlt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val root: File, val logger: Logger) {

    companion object {
        val VLT_FILE = ".vlt"

        val JCR_CONTENT_FILE = ".content.xml"

        val CONTENT_PROP_PATTERN = Pattern.compile("([^=]+)=\"([^\"]+)\"")
    }

    fun removeVltFiles() {
        for (file in FileUtils.listFiles(root, NameFileFilter(VLT_FILE), TrueFileFilter.INSTANCE)) {
            logger.info("Deleting {}", file.path)
            FileUtils.deleteQuietly(file)
        }
    }

    fun cleanupDotContent(props: List<String>, lineEnding: String) {
        val contentProperties = VltContentProperty.manyFrom(props)

        for (file in FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)) {
            try {
                logger.info("Cleaning up {}", file.path)

                val inputLines = FileUtils.readLines(file, CharEncoding.UTF_8)
                val filteredLines = filterLines(file, inputLines, contentProperties)

                FileUtils.writeLines(file, CharEncoding.UTF_8, filteredLines, lineEnding)
            } catch (e: IOException) {
                throw VltException(String.format("Error opening %s", file.path), e)
            }
        }
    }

    private fun filterLines(file: File, lines: List<String>, props: List<VltContentProperty>): List<String> {
        val result = mutableListOf<String>()

        for (line in lines) {
            val cleanLine = StringUtils.trimToEmpty(line)
            if (lineContainsProperty(file, line, props)) {
                when {
                    cleanLine.endsWith("/>") -> {
                        result.add(result.removeAt(result.size - 1) + "/>")
                    }
                    cleanLine.endsWith(">") -> {
                        result.add(result.removeAt(result.size - 1) + ">")
                    }
                    else -> {
                        // skip line with property
                    }
                }
            } else {
                result.add(line)
            }
        }

        return result
    }

    private fun lineContainsProperty(file: File, line: String, props: List<VltContentProperty>): Boolean {
        val normalizedLine = line.trim().removeSuffix("/>").removeSuffix(">")
        val matcher = CONTENT_PROP_PATTERN.matcher(normalizedLine)
        if (matcher.matches()) {
            val propOccurence = matcher.group(1)

            props.any { it.match(file, propOccurence) }
        }

        return false
    }
}

