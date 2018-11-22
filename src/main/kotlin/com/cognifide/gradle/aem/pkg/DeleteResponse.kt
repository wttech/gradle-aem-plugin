package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.internal.http.ResponseException
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

        private val STATUS_TAGS = listOf(DELETE_SUCCESS)

        fun from(input: InputStream, bufferSize: Int): DeleteResponse {
            return try {
                DeleteResponse(readFrom(input, ERROR_PATTERNS, STATUS_TAGS, bufferSize))
            } catch (e: Exception) {
                throw ResponseException("Malformed delete package response.")
            }
        }
    }
}
