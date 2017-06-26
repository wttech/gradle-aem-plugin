package com.cognifide.gradle.aem.deploy.response

import com.cognifide.gradle.aem.deploy.ErrorPattern
import java.util.regex.Pattern

class UninstallResponse(private val rawHtml: String) : AbstractHtmlResponse(rawHtml) {

    companion object {
        val UNINSTALL_SUCCESS = "<span class=\"Uninstalling package from"

        val PACKAGE_MISSING: Pattern = Pattern.compile("<span class=\"Unable to revert package content. Snapshot missing")
    }

    override fun getErrorPatterns(): List<ErrorPattern> {
        return mutableListOf(
                ErrorPattern(PACKAGE_MISSING, false, "Package is not installed.")
        )
    }

    val status: AbstractHtmlResponse.Status
        get() {
            return if (rawHtml.contains(UNINSTALL_SUCCESS)) {
                AbstractHtmlResponse.Status.SUCCESS
            } else {
                AbstractHtmlResponse.Status.FAIL
            }
        }
}
