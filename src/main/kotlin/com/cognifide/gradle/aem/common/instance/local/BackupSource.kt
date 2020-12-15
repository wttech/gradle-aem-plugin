package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.common.file.transfer.FileEntry
import java.io.File

class BackupSource(val type: BackupType, val fileEntry: FileEntry, val fileResolver: () -> File) {

    val file: File get() = fileResolver()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupSource

        if (type != other.type) return false
        if (fileEntry != other.fileEntry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + fileEntry.hashCode()
        return result
    }
}
