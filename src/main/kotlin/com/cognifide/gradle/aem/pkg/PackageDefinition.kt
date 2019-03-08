package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.pkg.vlt.VltDefinition
import java.io.File

class PackageDefinition(aem: AemExtension) : VltDefinition(aem) {

    internal val contentCallbacks = mutableListOf<(File) -> Unit>()

    fun content(callback: (File) -> Unit) {
        contentCallbacks += callback
    }
}