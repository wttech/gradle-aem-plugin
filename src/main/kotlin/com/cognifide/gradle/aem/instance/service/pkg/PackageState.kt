package com.cognifide.gradle.aem.instance.service.pkg

import java.io.File

class PackageState(val file: File, val state: Package?) {

    val name: String
        get() = file.name

    val uploaded: Boolean
        get() = state != null

    val installed: Boolean
        get() = state != null && state.installed
}