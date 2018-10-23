package com.cognifide.gradle.aem.pkg.deploy

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

        fun from(html: String): DeleteResponse {
            return try {
                DeleteResponse(html)
            } catch (e: Exception) {
                throw ResponseException("Malformed delete package response.")
            }
        }

    }
}
