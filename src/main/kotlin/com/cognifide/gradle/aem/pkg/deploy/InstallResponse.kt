package com.cognifide.gradle.aem.pkg.deploy

import java.io.InputStream
import java.util.regex.Pattern

class InstallResponse constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return mutableListOf(
                ErrorPattern(PROCESSING_ERROR_PATTERN, true),
                ErrorPattern(ERROR_PATTERN, false)
        )
    }

    override val status: Status
        get() {
            return when {
                rawHtml.contains(INSTALL_SUCCESS) -> Status.SUCCESS
                rawHtml.contains(INSTALL_SUCCESS_WITH_ERRORS) -> Status.SUCCESS_WITH_ERRORS
                else -> Status.FAIL
            }
        }

    companion object {

        const val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

        const val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"

        val ERROR_PATTERN: Pattern =
                Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

        val PROCESSING_ERROR_PATTERN: Pattern =
                Pattern.compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

        fun from(input: InputStream): InstallResponse {
            return try {
                //TODO replace empty object with proper logic
                val empty = InstallResponse("")
                InstallResponse(readFrom(input,empty.getErrorPatterns(), listOf(INSTALL_SUCCESS, INSTALL_SUCCESS_WITH_ERRORS)))
            } catch (e: Exception) {
                throw ResponseException("Malformed install package response.")
            }
        }

    }

}
