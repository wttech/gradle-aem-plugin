package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.common.AemPlugin
import com.cognifide.gradle.aem.common.Patterns
import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileOperations {

    fun readResource(path: String): InputStream? {
        return javaClass.getResourceAsStream("/${AemPlugin.PKG.replace(".", "/")}/$path")
    }

    fun getResources(path: String): List<String> {
        return Reflections("${AemPlugin.PKG}.$path".replace("/", "."), ResourcesScanner()).getResources { true; }.toList()
    }

    fun eachResource(resourceRoot: String, targetDir: File, callback: (String, File) -> Unit) {
        for (resourcePath in getResources(resourceRoot)) {
            val outputFile = File(targetDir, resourcePath.substringAfterLast("$resourceRoot/"))

            callback(resourcePath, outputFile)
        }
    }

    fun copyResources(resourceRoot: String, targetDir: File, skipExisting: Boolean = false) {
        eachResource(resourceRoot, targetDir) { resourcePath, outputFile ->
            if (!skipExisting || !outputFile.exists()) {
                copyResource(resourcePath, outputFile)
            }
        }
    }

    fun copyResource(resourcePath: String, outputFile: File) {
        GFileUtils.mkdirs(outputFile.parentFile)

        javaClass.getResourceAsStream("/$resourcePath").use { input ->
            FileOutputStream(outputFile).use { output ->
                IOUtils.copy(input, output)
            }
        }
    }

    fun amendFiles(dir: File, wildcardFilters: List<String>, amender: (File, String) -> String) {
        val files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
        files?.filter { Patterns.wildcard(it, wildcardFilters) }?.forEach { file ->
            val source = amender(file, file.inputStream().bufferedReader().use { it.readText() })
            file.printWriter().use { it.print(source) }
        }
    }

    fun amendFile(file: File, amender: (String) -> String) {
        val source = amender(file.inputStream().bufferedReader().use { it.readText() })
        file.printWriter().use { it.print(source) }
    }

    fun find(project: Project, dirIfFileName: String, pathOrFileNames: List<String>): File? {
        for (pathOrFileName in pathOrFileNames) {
            val file = find(project, dirIfFileName, pathOrFileName)
            if (file != null) {
                return file
            }
        }

        return null
    }

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

    /**
     * Only Zip4j correctly extracts AEM backup ZIP files.
     * Gradle zipTree and Zero-Turnaround ZipUtil is not working properly in that case.
     */
    fun zipUnpack(zip: File, targetDir: File) {
        ZipFile(zip).extractAll(targetDir.absolutePath)
    }
}