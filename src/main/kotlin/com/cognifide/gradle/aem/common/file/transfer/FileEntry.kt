package com.cognifide.gradle.aem.common.file.transfer

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

data class FileEntry(
    val name: String,
    val modified: Long? = null,
    val size: Long? = null
) {

    companion object {

        fun of(file: File): FileEntry {
            val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            return FileEntry(file.name, attributes.size(), attributes.lastModifiedTime().toMillis())
        }
    }
}