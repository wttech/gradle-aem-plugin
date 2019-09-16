package com.cognifide.gradle.aem.common.file.transfer

import java.io.File

data class FileEntry(
    val name: String,
    val size: Long? = null,
    val modified: Long? = null
) {

    companion object {

        fun of(file: File): FileEntry = file.run { FileEntry(name, length(), lastModified()) }
    }
}
