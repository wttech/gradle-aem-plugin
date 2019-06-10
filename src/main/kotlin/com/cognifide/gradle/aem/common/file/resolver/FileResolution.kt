package com.cognifide.gradle.aem.common.file.resolver

import java.io.File
import org.gradle.api.file.FileTree

open class FileResolution(val group: FileGroup, val id: String, private val resolver: (FileResolution) -> File) {

    private val aem = group.aem

    val dir = File("${group.downloadDir}/$id")

    val file: File by lazy { thenOperations.fold(resolver(this)) { f, o -> o(f) } }

    private var thenOperations = mutableListOf<FileResolution.(File) -> File>()

    fun then(operation: FileResolution.(File) -> File) {
        thenOperations.add(operation)
    }

    fun copyFile(source: File, target: File) {
        if (!target.exists()) {
            source.copyTo(target)
        }
    }

    fun archiveTree(archive: File): FileTree = when (archive.extension) {
        "zip" -> aem.project.zipTree(archive)
        else -> aem.project.tarTree(archive)
    }

    fun archiveEntry(archive: File, entryPath: String): File = archiveTree(archive)
            .matching { it.include(entryPath) }.singleFile

    fun copyArchiveEntry(archive: File, entryPath: String, target: File) {
        if (!target.exists()) {
            archiveEntry(archive, entryPath).copyTo(target)
        }
    }
}