package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

object FileOperations {

    fun amendFile(file: File, amender: (String) -> String) {
        val source = amender(file.inputStream().bufferedReader().use { it.readText() })
        file.printWriter().use { it.print(source) }
    }

    fun find(project: Project, dir: File, fileNames: List<String>) = find(project, dir.toString(), fileNames)

    fun find(project: Project, dirIfFileName: String, pathOrFileNames: List<String>): File? {
        for (pathOrFileName in pathOrFileNames) {
            val file = find(project, dirIfFileName, pathOrFileName)
            if (file != null) {
                return file
            }
        }

        return null
    }

    fun find(project: Project, dir: File, pathOrFileName: String) = find(project, dir.toString(), pathOrFileName)

    fun find(project: Project, dirIfFileName: String, pathOrFileName: String): File? {
        if (pathOrFileName.isBlank()) {
            return null
        }

        return mutableListOf<(String) -> File>(
                { project.file(pathOrFileName) },
                { File(File(dirIfFileName), pathOrFileName) },
                { File(pathOrFileName) }
        ).map { it(pathOrFileName) }.firstOrNull { it.exists() }
    }

    fun find(dir: File, pattern: String): File? {
        return find(dir, listOf(pattern))
    }

    fun find(dir: File, patterns: List<String>): File? {
        var result: File? = null
        val files = dir.listFiles { _, name -> Patterns.wildcard(name, patterns) }
        if (files != null) {
            result = files.firstOrNull()
        }
        return result
    }

    fun isDirEmpty(dir: File): Boolean {
        return dir.exists() && isDirEmpty(Paths.get(dir.absolutePath))
    }

    fun isDirEmpty(dir: Path): Boolean {
        Files.newDirectoryStream(dir).use { dirStream -> return !dirStream.iterator().hasNext() }
    }

    fun removeDirContents(dir: File): Boolean {
        val children = dir.listFiles() ?: arrayOf()
        var result = true
        children.forEach {
            result = result && it.deleteRecursively()
        }
        return result
    }

    fun lock(file: File) {
        file.parentFile.mkdirs()
        file.writeText(Formats.toJson(mapOf("locked" to Formats.date())))
    }

    fun lock(file: File, callback: () -> Unit) {
        if (!file.exists()) {
            callback()
            lock(file)
        }
    }

    fun makeExecutable(file: File) {
        Files.setPosixFilePermissions(file.toPath(), Files.getPosixFilePermissions(file.toPath()) + setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE
        ))
    }
}
