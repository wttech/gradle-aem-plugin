package com.cognifide.gradle.aem.pkg.deploy

import java.io.InputStream
import java.util.regex.Pattern

class UninstallResponse private constructor(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return mutableListOf(ErrorPattern(PACKAGE_MISSING, false, "Package is not installed."))
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
        const val UNINSTALL_SUCCESS = "<span class=\"Uninstalling package from"

        val PACKAGE_MISSING: Pattern = Pattern.compile("<span class=\"Unable to revert package content. Snapshot missing")

        fun from(input: InputStream): UninstallResponse {
            return try {
                //TODO empty
                val empty = UninstallResponse("")
                UninstallResponse(readFrom(input,empty.getErrorPatterns(), listOf(UNINSTALL_SUCCESS)))
            } catch (e: Exception) {
                throw ResponseException("Malformed uninstall package response.")
            }
        }
    }
}
