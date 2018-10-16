package com.cognifide.gradle.aem.pkg.deploy

import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.util.regex.Pattern

object InstallResponseBuilder {

    val maxBytesToReadAtOnce = 2000000

    val numberOfLinesPerPart = 5000

    val ERROR_PATTERN = Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

    val PROCESSING_ERROR_PATTERN = Pattern.compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

    val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

    val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"

    val errors = mutableListOf(
            ErrorPattern(InstallResponseBuilder.PROCESSING_ERROR_PATTERN, true),
            ErrorPattern(InstallResponseBuilder.ERROR_PATTERN, false))


    fun buildFromStream(stream: InputStream): InstallResponse {
        val size = stream.available()
        return if (size <= maxBytesToReadAtOnce) {
            InstallResponse(IOUtils.toString(stream))
        } else {
            readStreamPartially(stream)
        }
    }

    fun readStreamPartially(stream: InputStream): InstallResponse {
        val buf = IOUtils.toBufferedInputStream(stream)
        val reader = buf.bufferedReader()

        var lineBuilder = StringBuilder()
        var nextCharacter: Int
        var lines = 1

        val resultBuilder = StringBuilder()


        do {
            nextCharacter = reader.read()
            lineBuilder.append(nextCharacter.toChar())
            if (nextCharacter == 10) {
                lines++
            }
            if (lines % numberOfLinesPerPart == 0) {

                extractSignificantData(lineBuilder.toString(), resultBuilder)
                lineBuilder = StringBuilder()
                lines++
            }


        } while (nextCharacter != -1)
        extractSignificantData(lineBuilder.toString(), resultBuilder)

        val result = InstallResponse(resultBuilder.toString())

        return result
    }

    private fun extractSignificantData(line: String, builder: StringBuilder) {
        InstallResponseBuilder.errors.forEach {
            val matcher = it.pattern.matcher(line)
            while (matcher.find()) {
                builder.append(matcher.group() + "\n\n")
            }
            when {
                line.contains(INSTALL_SUCCESS) -> builder.append(INSTALL_SUCCESS)
                line.contains(INSTALL_SUCCESS_WITH_ERRORS) -> builder.append(INSTALL_SUCCESS_WITH_ERRORS)
            }
        }
    }
}