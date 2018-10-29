package com.cognifide.gradle.aem.pkg.deploy

import java.io.InputStream
import java.util.regex.Pattern

class InstallResponse private constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return ERROR_PATTERNS
    }

    override val status: Status
        get() {
            return when {
                rawHtml.contains(INSTALL_SUCCESS) -> Status.SUCCESS
                rawHtml.contains(INSTALL_SUCCESS_WITH_ERRORS) -> Status.SUCCESS_WITH_ERRORS
                else -> Status.FAIL
            }
        }

    fun findPackageErrors(packageErrors:Collection<String>): Set<String> {
        return errors.fold(mutableSetOf())
            { results, error ->
                packageErrors.forEach { packageError ->
                    if (error.contains(packageError)) results.add(packageError)
                }; results
            }
    }

    companion object {

        private const val INSTALL_SUCCESS = "<span class=\"Package imported.\">"

        private const val INSTALL_SUCCESS_WITH_ERRORS = "<span class=\"Package imported (with errors"

        private val ERROR_PATTERN: Pattern =
                Pattern.compile("<span class=\"E\"><b>E</b>&nbsp;(.+\\s??.+)</span>")

        private val PROCESSING_ERROR_PATTERN: Pattern = Pattern.compile("<span class=\"error\">(.+)</span><br><code><pre>([\\s\\S]+)</pre>")

        private val ERROR_PATTERNS = listOf(
                ErrorPattern(PROCESSING_ERROR_PATTERN, true),
                ErrorPattern(ERROR_PATTERN, false))

        private val STATUS_TAGS = listOf(INSTALL_SUCCESS, INSTALL_SUCCESS_WITH_ERRORS)

        fun from(input: InputStream, bufferSize: Int): InstallResponse {
            return try {
                InstallResponse(readFrom(input, ERROR_PATTERNS, STATUS_TAGS,bufferSize))
            } catch (e: Exception) {
                throw ResponseException("Malformed install package response.")
            }
        }

    }

}
