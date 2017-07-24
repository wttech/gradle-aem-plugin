package com.cognifide.gradle.aem.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.commons.validator.routines.UrlValidator

object Formats {

    val URL_VALIDATOR = UrlValidator(arrayOf("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

    fun toJson(value: Any): String? {
        return ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(value)
    }

}