package com.cognifide.gradle.aem.common.instance.local

import java.io.File

class BackupSource(val name: String, val fileResolver: () -> File) {

    val file: File
        get() = fileResolver()
}