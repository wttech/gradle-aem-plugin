package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.util.*

class VltCleaner(val root: File, val logger: Logger) {

    companion object {
        val VLT_FILE = ".vlt"

        val JCR_CONTENT_FILE = ".content.xml"

        fun clean(project : Project) {
            val config = AemConfig.of(project)
            val contentDir = File(config.contentPath)

            if (!contentDir.exists()) {
                project.logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
                return
            }

            val cleaner = VltCleaner(contentDir, project.logger)
            cleaner.removeVltFiles()
            cleaner.cleanupDotContent(config.vaultSkipProperties, config.vaultLineSeparator)
        }
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

                FileUtils.writeLines(file, CharEncoding.UTF_8, lines, lineEnding)
            } catch (e: IOException) {
                throw VltException(String.format("Error opening %s", file.path), e)
            }

        }
    }

    private fun lineContainsProperty(cleanLine: String, contentProperties: List<String>): Boolean {
        return contentProperties.any { cleanLine.startsWith(it) }
    }

}

