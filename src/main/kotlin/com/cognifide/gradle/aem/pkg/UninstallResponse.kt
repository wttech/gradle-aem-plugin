package com.cognifide.gradle.aem.pkg

import java.io.InputStream
import java.util.regex.Pattern

class UninstallResponse private constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return ERROR_PATTERNS
    }

    override val status: Status
        get() {
            return if (rawHtml.contains(UNINSTALL_SUCCESS)) {
                Status.SUCCESS
            } else {
                Status.FAIL
            }
        }

    companion object {
        private const val UNINSTALL_SUCCESS = "<span class=\"Uninstalling package from"

        private val PACKAGE_MISSING: Pattern = Pattern.compile("<span class=\"Unable to revert package content. Snapshot missing")

        private val ERROR_PATTERNS = listOf(ErrorPattern(PACKAGE_MISSING, false, "Package is not installed."))

        private val STATUS_TAGS = listOf(UNINSTALL_SUCCESS)

        fun from(input: InputStream, bufferSize: Int): UninstallResponse {
            return try {
                UninstallResponse(readFrom(input, ERROR_PATTERNS, STATUS_TAGS, bufferSize))
            } catch (e: Exception) {
                throw ResponseException("Malformed uninstall package response.")
            }
        }
    }
}
