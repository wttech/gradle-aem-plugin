package com.cognifide.gradle.aem.pkg.deploy

import java.io.InputStream

class DeleteResponse private constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return emptyList()
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

        const val DELETE_SUCCESS = "Package deleted in"

        fun from(input: InputStream): DeleteResponse {
            return try {
                //TODO empty
                val empty = DeleteResponse("")
                DeleteResponse(readFrom(input, empty.getErrorPatterns(), listOf(DELETE_SUCCESS)))
            } catch (e: Exception) {
                throw ResponseException("Malformed delete package response.")
            }
        }

    }
}
