package com.cognifide.gradle.aem.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

object Formats {

    fun toJson(value: Any): String? {
        return ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(value)
    }

}