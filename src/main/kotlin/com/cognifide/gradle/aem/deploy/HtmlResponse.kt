package com.cognifide.gradle.aem.deploy


import java.util.regex.Pattern

abstract class HtmlResponse(private val rawHtml: String) {

    enum class Status {
        FAIL, SUCCESS, SUCCESS_WITH_ERRORS
    }

    abstract val status: Status

    private val _errors: MutableList<String> = mutableListOf()

    protected abstract fun getErrorPatterns(): List<ErrorPattern>

    init {
        getErrorPatterns().forEach({
            findErrorsByPattern(it.pattern, it.printStackTrace, it.message)
        })
    }

    private fun findErrorsByPattern(pattern: Pattern, printStacktrace: Boolean, message: String) {
        val matcher = pattern.matcher(rawHtml)

        while (matcher.find()) {

            if (!message.isNullOrBlank()) {
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
}

