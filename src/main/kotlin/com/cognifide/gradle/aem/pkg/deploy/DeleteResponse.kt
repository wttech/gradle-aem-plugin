package com.cognifide.gradle.aem.pkg.deploy

import java.io.InputStream

class DeleteResponse private constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return ERROR_PATTERNS
    }

    override val status: Status
        get() {
            return if (rawHtml.contains(DELETE_SUCCESS)) {
                Status.SUCCESS
            } else {
                Status.FAIL
            }
        }

    companion object {

        private const val DELETE_SUCCESS = "Package deleted in"

        private val ERROR_PATTERNS = emptyList<ErrorPattern>()

        fun from(input: InputStream): DeleteResponse {
            return try {
                val statusTags = listOf(DELETE_SUCCESS)
                DeleteResponse(readFrom(input, ERROR_PATTERNS, statusTags))
            } catch (e: Exception) {
                throw ResponseException("Malformed delete package response.")
            }
        }

    }
}
