package com.cognifide.gradle.aem.pkg.deploy

import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.InputStream
import java.util.regex.Pattern

abstract class HtmlResponse(private val rawHtml: String) {

    enum class Status {
        FAIL, SUCCESS, SUCCESS_WITH_ERRORS
    }

    abstract val status: Status

    private val _errors: MutableList<String> = mutableListOf()

    protected abstract fun getErrorPatterns(): List<ErrorPattern>

    init {
        this.getErrorPatterns().forEach {
            findErrorsByPattern(it.pattern, it.printStackTrace, it.message)
        }
    }

    private fun findErrorsByPattern(pattern: Pattern, printStacktrace: Boolean, message: String) {
        val matcher = pattern.matcher(rawHtml)

        while (matcher.find()) {

            if (!message.isBlank()) {
                _errors.add(message)
            }

            if (matcher.groupCount() > 0) {
                val error = matcher.group(1)
                _errors.add(error)
            }

            if (printStacktrace) {
                if (matcher.groupCount() > 1) {
                    val secondGroup = matcher.group(2)
                    if (!secondGroup.isNullOrBlank()) {
                        _errors.add(secondGroup)
                    }
                }
            }
        }
    }

    val errors: List<String> = _errors.toList()

    val success: Boolean
        get() = status == Status.SUCCESS

    companion object {
        private const val NUMBER_OF_LINES_TO_READ = 5000

        private const val ERROR_SEPARATOR = "\n\n"

        private const val LINE_FEED = 10.toChar()

        fun readFrom(stream: InputStream, errorPatterns: List<ErrorPattern>, statusTags: List<String>): String {
            val buf = IOUtils.toBufferedInputStream(stream)
            val reader = buf.bufferedReader()
            return readByLines(reader,errorPatterns,statusTags)
        }

        private fun readByLines(input: BufferedReader, errorPatterns: List<ErrorPattern>, statusTags: List<String>): String {
            val resultBuilder = StringBuilder()
            val chunk = StringBuilder()
            var currentLine = 0
            input.forEachLine {
                chunk.append(it + LINE_FEED)
                currentLine++
                if (currentLine % NUMBER_OF_LINES_TO_READ == 0) {
                    extractErrors(chunk, resultBuilder, errorPatterns, statusTags)
                }
            }
            extractErrors(chunk, resultBuilder, errorPatterns, statusTags)
            return resultBuilder.toString()
        }

        private fun extractErrors(chunk: StringBuilder, builder: StringBuilder, errorPatterns: List<ErrorPattern>, statusTags: List<String>) {
            errorPatterns.forEach {
                val matcher = it.pattern.matcher(chunk)
                while (matcher.find()) {
                    builder.append("${matcher.group()}$ERROR_SEPARATOR")
                }
            }
            statusTags.forEach {
                if (chunk.contains(it)) builder.append(it)
            }
            chunk.setLength(0)
        }
    }

}

