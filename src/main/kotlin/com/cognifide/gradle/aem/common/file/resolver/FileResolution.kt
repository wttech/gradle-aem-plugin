package com.cognifide.gradle.aem.common.file.resolver

import java.io.File
import org.gradle.api.file.FileTree

open class FileResolution(val group: FileGroup, val id: String, private val resolver: (FileResolution) -> File) {

    private val aem = group.aem

    val dir = File("${group.downloadDir}/$id")

    val file: File by lazy { thenOperations.fold(resolver(this)) { f, o -> o(f) } }

    private var thenOperations = mutableListOf<FileResolution.(File) -> File>()

    /**
     * Perform operation on resolved file, but do not change it path (work in-place).
     */
    fun use(operation: FileResolution.(File) -> Unit) {
        then { operation(it); it }
    }

    /**
     * Perform operation on resolved file with ability to change it path.
     */
    fun then(operation: FileResolution.(File) -> File) {
        thenOperations.add(operation)
    }

    // DSL for 'then' and 'use' methods

    /**
     * Copy source file to target only if it does not exist.
     */
    fun copyFile(source: File, target: File) {
        if (!target.exists()) {
            aem.logger.info("Copying resolved file '$source' to file '$target'")

            source.copyTo(target)
        }
    }

    /**
     * Read files from ZIP/TAR archive.
     */
    fun archiveTree(archive: File): FileTree = when (archive.extension) {
        "zip" -> aem.project.zipTree(archive)
        else -> aem.project.tarTree(archive)
    }

    /**
     * Read single file from ZIP/TAR archive.
     */
    fun archiveFile(archive: File, entryPath: String): File = archiveTree(archive)
            .matching { it.include(entryPath) }.singleFile

    /**
     * Extract & copy single archive file and copy it to target location only if it does not exist.
     */
    fun copyArchiveFile(archive: File, entryPath: String, target: File) = target.apply {
        if (!exists()) {
            aem.logger.info("Copying resolved archive file '$archive' to file '$target'")

            archiveFile(archive, entryPath).copyTo(this)
        }
    }
}