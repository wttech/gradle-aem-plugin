package com.cognifide.gradle.aem.common.instance.local

import java.io.File

class BackupSource(
    val type: BackupType,
    val fileName: String,
    val fileResolver: () -> File
) {

    val file: File
        get() = fileResolver()
}