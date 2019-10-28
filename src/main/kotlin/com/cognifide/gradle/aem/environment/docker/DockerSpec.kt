package com.cognifide.gradle.aem.environment.docker

import java.io.InputStream
import java.io.OutputStream

interface DockerSpec {

    var command: String

    val args: List<String>

    val fullCommand: String

    var options: List<String>

    var exitCodes: List<Int>

    var input: InputStream?

    var output: OutputStream?

    var errors: OutputStream?

    fun option(value: String)

    fun ignoreExitCodes()
}
