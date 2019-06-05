package com.cognifide.gradle.aem.common.instance.local

import java.io.File

class Script(val wrapper: File, val bin: File, val command: List<String>) {

    val commandLine: List<String>
        get() = command + listOf(wrapper.absolutePath)

    override fun toString(): String {
        return "Script(commandLine=$commandLine)"
    }
}