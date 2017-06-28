package com.cognifide.gradle.aem.deploy

import java.util.regex.Pattern

class InstallResponse(private val rawHtml: String) {

    companion object {
        val ERROR_PATTERN = Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

        val PROCESSING_ERROR_PATTERN = Pattern
                .compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

        val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

        val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"
    }

    enum class Status {
        FAIL, SUCCESS, SUCCESS_WITH_ERRORS
    }

    private val _errors: MutableList<String> = mutableListOf()

    init {
        findErrorsByPattern(PROCESSING_ERROR_PATTERN, true)
        findErrorsByPattern(ERROR_PATTERN, false)
    }

    private fun findErrorsByPattern(pattern: Pattern, printStacktrace: Boolean) {
        val matcher = pattern.matcher(rawHtml)

        while (matcher.find()) {
            val error = matcher.group(1)

            _errors.add(error)

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

    val status: Status
        get() {
            return if (rawHtml.contains(INSTALL_SUCCESS)) {
                Status.SUCCESS
            } else if (rawHtml.contains(INSTALL_SUCCESS_WITH_ERRORS)) {
                Status.SUCCESS_WITH_ERRORS
            } else {
                Status.FAIL
            }
        }
}
