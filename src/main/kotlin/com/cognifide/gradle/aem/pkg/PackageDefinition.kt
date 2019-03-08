package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.pkg.vlt.VltDefinition
import java.io.File

class PackageDefinition(aem: AemExtension) : VltDefinition(aem) {

    private var fileCustom: File? = null

    var file: File
        get() = fileCustom ?: File(aem.temporaryDir, "$group-$name-$version.zip")
        set(value) {
            fileCustom = value
        }

    val dir: File
        get() = File(file.parentFile, file.nameWithoutExtension)

    internal val contentCallbacks = mutableListOf<(File) -> Unit>()

    fun content(callback: (File) -> Unit) {
        contentCallbacks += callback
    }
}