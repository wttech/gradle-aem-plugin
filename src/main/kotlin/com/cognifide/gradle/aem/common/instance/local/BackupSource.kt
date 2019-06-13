package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import java.io.File

class BackupSource(
    val type: BackupType,
    val fileEntry: FileEntry,
    val fileResolver: () -> File
) {

    val file: File
        get() = fileResolver()
}