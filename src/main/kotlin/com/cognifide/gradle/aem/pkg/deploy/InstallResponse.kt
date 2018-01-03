package com.cognifide.gradle.aem.pkg.deploy

import java.util.regex.Pattern

class InstallResponse(private val rawHtml: String) : HtmlResponse(rawHtml) {

    companion object {
        val ERROR_PATTERN = Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

        val PROCESSING_ERROR_PATTERN = Pattern
                .compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

        val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

        val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"
    }

    override fun getErrorPatterns(): List<ErrorPattern> {
        return mutableListOf(
                ErrorPattern(PROCESSING_ERROR_PATTERN, true),
                ErrorPattern(ERROR_PATTERN, false)
        )
    }

    override val status: Status
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
