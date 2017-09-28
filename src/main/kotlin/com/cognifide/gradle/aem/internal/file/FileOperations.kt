package com.cognifide.gradle.aem.internal.file

import com.cognifide.gradle.aem.AemBasePlugin
import com.cognifide.gradle.aem.internal.Patterns
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileOperations {

    fun getResources(path: String): List<String> {
        return Reflections("${AemBasePlugin.PKG}.$path".replace("/", "."), ResourcesScanner()).getResources { true; }.toList()
    }

    fun eachResource(resourceRoot: String, targetDir: File, callback: (String, File) -> Unit) {
        for (resourcePath in getResources(resourceRoot)) {
            val outputFile = File(targetDir, resourcePath.substringAfterLast("$resourceRoot/"))

            callback(resourcePath, outputFile)
        }
    }

    fun copyResources(resourceRoot: String, targetDir: File, skipExisting: Boolean = false) {
        eachResource(resourceRoot, targetDir, { resourcePath, outputFile ->
            if (!skipExisting || !outputFile.exists()) {
                copyResource(resourcePath, outputFile)
            }
        })
    }

    fun copyResource(resourcePath: String, outputFile: File) {
        GFileUtils.mkdirs(outputFile.parentFile)

        val input = javaClass.getResourceAsStream("/" + resourcePath)
        val output = FileOutputStream(outputFile)

        try {
            IOUtils.copy(input, output)
        } finally {
            IOUtils.closeQuietly(input)
            IOUtils.closeQuietly(output)
        }
    }

    fun amendFiles(dir: File, wildcardFilters: List<String>, amender: (File, String) -> String) {
        val files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
        files?.filter { Patterns.wildcard(it, wildcardFilters) }?.forEach { file ->
            val buffer = StringWriter()
            file.inputStream().bufferedReader().use { it.forEachLine { line ->
                buffer.write(amender(file, line) + System.lineSeparator())
            }}
            file.printWriter().use { it.print(buffer.toString()) }
        }
    }

    fun amendFile(file: File, amender: (String) -> String) {
        val source = amender(file.inputStream().bufferedReader().use { it.readText() })
        file.printWriter().use { it.print(source) }
    }

    fun find(project: Project, dirIfFileName: String, pathOrFileName: String): File? {
        return mutableListOf<(String) -> File>(
                { project.file(pathOrFileName) },
                { File(File(dirIfFileName), pathOrFileName) }
        ).map { it(pathOrFileName) }.firstOrNull { it.exists() }
    }

    fun find(dir: File, patterns: List<String>): File? {
        var result: File? = null
        val files = dir.listFiles({ _, name -> Patterns.wildcard(name, patterns) })
        if (files != null) {
            result = files.firstOrNull()
        }
        return result
    }

    fun isDirEmpty(dir: File): Boolean {
        return dir.exists() && isDirEmpty(Paths.get(dir.absolutePath))
    }

    fun isDirEmpty(dir: Path): Boolean {
        Files.newDirectoryStream(dir).use({ dirStream -> return !dirStream.iterator().hasNext() })
    }

}