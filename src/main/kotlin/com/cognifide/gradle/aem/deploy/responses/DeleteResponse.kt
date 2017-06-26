package com.cognifide.gradle.aem.deploy.responses

import com.cognifide.gradle.aem.deploy.ErrorPattern

class DeleteResponse(private val rawHtml: String) : AbstractHtmlResponse(rawHtml) {

    companion object {
        val DELETE_SUCCESS = "Package deleted in"
    }

    override fun getErrorPatterns(): List<ErrorPattern> {
        return emptyList()
    }

    val status: AbstractHtmlResponse.Status
        get() {
            return if (rawHtml.contains(DELETE_SUCCESS)) {
                AbstractHtmlResponse.Status.SUCCESS
            } else {
                AbstractHtmlResponse.Status.FAIL
            }
        }
}
