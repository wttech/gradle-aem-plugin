package com.cognifide.gradle.aem.pkg.deploy

class InstallResponse(private val rawHtml: String) : HtmlResponse(rawHtml) {

    override fun getErrorPatterns(): List<ErrorPattern> {
        return mutableListOf(
                ErrorPattern(InstallResponseBuilder.PROCESSING_ERROR_PATTERN, true),
                ErrorPattern(InstallResponseBuilder.ERROR_PATTERN, false)
        )
    }

    override val status: Status
        get() {
            return when {
                rawHtml.contains(InstallResponseBuilder.INSTALL_SUCCESS) -> Status.SUCCESS
                rawHtml.contains(InstallResponseBuilder.INSTALL_SUCCESS_WITH_ERRORS) -> Status.SUCCESS_WITH_ERRORS
                else -> Status.FAIL
            }
        }
}
