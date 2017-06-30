package com.cognifide.gradle.aem.deploy

class DeleteResponse(private val rawHtml: String) : HtmlResponse(rawHtml) {

    companion object {
        val DELETE_SUCCESS = "Package deleted in"
    }

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
}
