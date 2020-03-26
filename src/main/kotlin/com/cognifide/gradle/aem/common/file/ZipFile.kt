package com.cognifide.gradle.aem.common.file

import net.lingala.zip4j.ZipFile as Base
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import org.apache.commons.lang3.StringUtils
import java.io.File

/**
 * Only Zip4j correctly extracts AEM backup ZIP files (files bigger than 10 GB)
 */
class ZipFile(val baseFile: File) {

    private val base = Base(baseFile)

    fun contains(fileName: String) = base.getFileHeader(fileName) != null

    fun containsDir(dirName: String) = base.getFileHeader(StringUtils.appendIfMissing(dirName, "/")) != null

    fun unpackAll(targetDir: File) {
        base.extractAll(targetDir.absolutePath)
    }

    fun packAll(sourceDir: File, options: ZipParameters.() -> Unit = {}) {
        val effectiveOptions = options(options)
        (sourceDir.listFiles() ?: arrayOf<File>()).forEach { f ->
            when {
                f.isDirectory -> base.addFolder(f, effectiveOptions)
                f.isFile -> base.addFile(f, effectiveOptions)
            }
        }
    }

    fun unpackDir(dirName: String, dir: File) {
        val dirFileName = "$dirName/"
        if (!contains(dirFileName)) {
            throw ZipException("ZIP file '$baseFile' does not contain directory '$dirName'!")
        }

        base.apply {
            (fileHeaders as List<FileHeader>).asSequence()
                    .filter { it.fileName.startsWith(dirFileName) }
                    .forEach { extractFile(it, dir.absolutePath) }
        }
    }

    fun unpackFile(fileName: String, targetFile: File) {
        if (!contains(fileName)) {
            throw ZipException("ZIP file '$baseFile' does not contain file '$fileName'!")
        }

        base.extractFile(fileName, targetFile.parentFile.absolutePath, targetFile.name)
    }

    fun unpackFileTo(fileName: String, targetDir: File) {
        unpackFile(fileName, targetDir.resolve(fileName.substringAfterLast("/")))
    }

    fun addDir(sourceDir: File, options: ZipParameters.() -> Unit = {}) {
        baseFile.parentFile.mkdirs()
        base.addFolder(sourceDir, options(options))
    }

    fun readFile(fileName: String): ZipInputStream = base.getFileHeader(fileName)?.let { base.getInputStream(it) }
            ?: throw ZipException("ZIP file '$baseFile' does not contain file '$fileName'!")

    fun readFileAsText(fileName: String) = readFile(fileName).use { it.bufferedReader().readText() }

    fun options(options: ZipParameters.() -> Unit) = ZipParameters().apply(OPTIONS_DEFAULT).apply(options)

    companion object {
        private val OPTIONS_DEFAULT: ZipParameters.() -> Unit = {
            compressionMethod = CompressionMethod.STORE
        }
    }
}
