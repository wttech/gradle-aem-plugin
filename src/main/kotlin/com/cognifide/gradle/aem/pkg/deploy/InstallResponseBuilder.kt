package com.cognifide.gradle.aem.pkg.deploy

import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.InputStream
import java.util.regex.Pattern

object InstallResponseBuilder {

    private const val NUMBER_OF_LINES_TO_READ = 5000

    private const val ERROR_SEPARATOR = "\n\n"

    private const val LINE_FEED = 10.toChar()

    const val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

    const val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"

    val ERROR_PATTERN: Pattern =
            Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

    val PROCESSING_ERROR_PATTERN: Pattern =
            Pattern.compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

    val errors = listOf(
            ErrorPattern(InstallResponseBuilder.PROCESSING_ERROR_PATTERN, true),
            ErrorPattern(InstallResponseBuilder.ERROR_PATTERN, false))


    fun buildFrom(stream: InputStream): InstallResponse {
        val buf = IOUtils.toBufferedInputStream(stream)
        val reader = buf.bufferedReader()
        val result = readByLines(reader)
        return InstallResponse.from(result)
    }

    private fun readByLines(input: BufferedReader): String {
        val resultBuilder = StringBuilder()
        val chunk = StringBuilder()
        var currentLine = 0
        input.forEachLine {
            chunk.append(it + LINE_FEED)
            currentLine++
            if (currentLine % NUMBER_OF_LINES_TO_READ == 0) {
                extractErrors(chunk, resultBuilder)
                chunk.setLength(0)
            }
        }
        extractErrors(chunk, resultBuilder)
        return resultBuilder.toString()
    }

    private fun extractErrors(chunk: StringBuilder, builder: StringBuilder) {
        InstallResponseBuilder.errors.forEach {
            val matcher = it.pattern.matcher(chunk)
            while (matcher.find()) {
                builder.append("${matcher.group()}$ERROR_SEPARATOR")
            }
        }
        when {
            chunk.contains(INSTALL_SUCCESS) -> builder.append(INSTALL_SUCCESS)
            chunk.contains(INSTALL_SUCCESS_WITH_ERRORS) -> builder.append(INSTALL_SUCCESS_WITH_ERRORS)
        }
    }
}

/*
TODO
- Proper throwing Package Exception
        - where to throw new?
        - create message
- Builder as companion object
+ Refactor builder reading method - try to forEach line
- Txt resource files with proper name
- No longer required gc
- Refactor test:
        - same responses?
        - package errors?
        - replace @Parametrized with junit5 features
- Remove large txt resource files
+ PackageErrors should be available to be manually extended via AemConfig.packageErrorExceptions
 */
