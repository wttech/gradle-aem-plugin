package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.common.utils.Formats
import java.io.InputStream
import java.io.OutputStream

open class DockerSpec {

    var command: String = ""

    open val args: List<String>
        get() = mutableListOf<String>().apply {
            addAll(options)
            addAll(Formats.commandToArgs(command))
        }

    val fullCommand: String
        get() = args.joinToString(" ")

    var options: List<String> = listOf()

    fun option(value: String) {
        options = options + value
    }

    var exitCodes: List<Int> = listOf(0)

    var input: InputStream? = null

    var output: OutputStream? = null

    var errors: OutputStream? = null
}
