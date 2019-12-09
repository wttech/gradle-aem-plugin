package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.AemExtension
import java.io.File
import java.util.*

class DaemonSpec(aem: AemExtension) : RunSpec(aem) {

    var initTime = 3_000L

    var outputFile: File? = null

    fun uniqueName() {
        name = UUID.randomUUID().toString()
    }

    fun outputFile(file: File) {
        outputFile = file
    }

    init {
        cleanup = true
    }
}
