package com.cognifide.gradle.aem.common.instance.service.groovy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.StringUtils

@JsonIgnoreProperties(ignoreUnknown = true)
class GroovyConsoleResult {

    lateinit var exceptionStackTrace: String

    lateinit var data: String

    lateinit var output: String

    lateinit var runningTime: String

    lateinit var script: String

    var result: String? = null

    @get:JsonIgnore
    val error: Boolean
        get() = exceptionStackTrace.isNotBlank()

    @get:JsonIgnore
    val success: Boolean
        get() = !error

    override fun toString(): String {
        return StringBuilder().apply {
            append("GroovyConsoleResult(output='${shorten(output)}', runningTime='$runningTime'")
            append(", exceptionStackTrace='${shorten(exceptionStackTrace)}', result='$result'")
            append(", script='${shorten(script)}', data='${shorten(data)}')")
        }.toString()
    }

    companion object {
        private const val ABBREVIATE_WIDTH = 200

        private fun shorten(text: String) = StringUtils.abbreviate(text.trim(), ABBREVIATE_WIDTH)
    }
}
