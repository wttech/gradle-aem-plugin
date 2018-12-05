package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class GroovyConsoleResult {

    lateinit var exceptionStackTrace: String

    lateinit var data: String

    lateinit var output: String

    lateinit var runningTime: String

    lateinit var script: String

    var result: String? = null
}