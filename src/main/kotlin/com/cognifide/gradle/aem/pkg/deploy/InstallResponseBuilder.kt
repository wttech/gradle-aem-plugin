package com.cognifide.gradle.aem.pkg.deploy

import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.InputStream
import java.util.regex.Pattern

object InstallResponseBuilder {

    private const val NUMBER_OF_LINES_TO_READ = 5000

    private const val ERROR_SEPARATOR = "\n\n"

    private const val LINE_FEED = 10

    const val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

    const val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"

    val ERROR_PATTERN: Pattern =
            Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

    val PROCESSING_ERROR_PATTERN: Pattern =
            Pattern.compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

    val errors = mutableListOf(
            ErrorPattern(InstallResponseBuilder.PROCESSING_ERROR_PATTERN, true),
            ErrorPattern(InstallResponseBuilder.ERROR_PATTERN, false))


    fun buildFromStream(stream: InputStream): InstallResponse {
        val buf = IOUtils.toBufferedInputStream(stream)
        val reader = buf.bufferedReader()
        val result = readByLines(reader)
        return InstallResponse(result)
    }

    private fun readByLines(source: BufferedReader): String {
        val lineBuilder = StringBuilder()
        val resultBuilder = StringBuilder()
        var currentLine = 0
        do {
            val nextCharacter = source.read()
            lineBuilder.append(nextCharacter.toChar())
            if (nextCharacter == LINE_FEED) {
                currentLine++
                if (currentLine % NUMBER_OF_LINES_TO_READ == 0) {
                    extractSignificantData(lineBuilder.toString(), resultBuilder)
                    lineBuilder.setLength(0)
                }
            }
        } while (nextCharacter != -1)
        extractSignificantData(lineBuilder.toString(), resultBuilder)
        return resultBuilder.toString()
    }

    private fun extractSignificantData(line: String, builder: StringBuilder) {
        InstallResponseBuilder.errors.forEach {
            val matcher = it.pattern.matcher(line)
            while (matcher.find()) {
                builder.append("${matcher.group()}$ERROR_SEPARATOR")
            }
            when {
                line.contains(INSTALL_SUCCESS) -> builder.append(INSTALL_SUCCESS)
                line.contains(INSTALL_SUCCESS_WITH_ERRORS) -> builder.append(INSTALL_SUCCESS_WITH_ERRORS)
            }
        }
    }
}