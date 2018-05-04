package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.Patterns
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val project: Project, val root: File) {

    private val logger = project.logger

    private val config = AemConfig.of(project)

    val dotContentProperties by lazy {
        VltContentProperty.manyFrom(config.cleanSkipProperties)
    }

    val dotContentFiles: Collection<File>
        get() = FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)
                ?: listOf()

    val allFiles: Collection<File>
        get() = FileUtils.listFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                ?: listOf()

    fun clean() {
        if (root.isDirectory) {
            for (file in allFiles) {
                removeFile(file)
            }

            for (file in dotContentFiles) {
                cleanDotContent(file)
            }
        } else {
            removeFile(root)
            cleanDotContent(root)
        }
    }

    private fun removeFile(file: File) {
        if (Patterns.wildcard(file, config.cleanFilesDeleted)) {
            logger.info("Deleting file {}", file.path)
            FileUtils.deleteQuietly(file)
        }
    }

    private fun cleanDotContent(file: File) {
        if (file.name != JCR_CONTENT_FILE) {
            return
        }

        try {
            logger.info("Cleaning file {}", file.path)

            val inputLines = FileUtils.readLines(file, CharEncoding.UTF_8)
            val filteredLines = filterLines(file, inputLines, dotContentProperties)

            FileUtils.writeLines(file, CharEncoding.UTF_8, filteredLines, config.vaultLineSeparatorString)
        } catch (e: IOException) {
            throw VltException(String.format("Error opening %s", file.path), e)
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

            return props.any { it.match(file, propOccurence) }
        }

        return false
    }

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^=]+)=\"([^\"]+)\"")
    }
}

