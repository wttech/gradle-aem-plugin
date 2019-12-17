package com.cognifide.gradle.aem.common.file.transfer.generic

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.AbstractFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import java.io.File
import java.io.IOException

/**
 * File transfer which is copying files instead of using them directly.
 */
class PathFileTransfer(aem: AemExtension) : AbstractFileTransfer(aem) {

    override val name: String
        get() = NAME

    override fun handles(fileUrl: String): Boolean = true

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        val fileUrl = "$dirUrl/$fileName"
        try {
            target.parentFile.mkdirs()
            file(dirUrl, fileName).apply { inputStream().use { downloader().download(length(), it, target) } }
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$fileUrl' to file '$target'. Cause: '${e.message}'", e)
        }
    }

    override fun uploadTo(dirUrl: String, fileName: String, source: File) {
        val fileUrl = "$dirUrl/$fileName"

        try {
            val target = file(dirUrl, fileName)
            target.parentFile.mkdirs()
            target.outputStream().use { uploader().upload(source, it) }
        } catch (e: IOException) {
            throw FileException("Cannot upload file '$source' to URL '$fileUrl'. Cause: '${e.message}", e)
        }
    }

    override fun list(dirUrl: String): List<FileEntry> = try {
        dirFiles(dirUrl).map { FileEntry.of(it) }
    } catch (e: IOException) {
        throw FileException("Cannot list files in directory at URL '$dirUrl'. Cause: '${e.message}'", e)
    }

    override fun deleteFrom(dirUrl: String, fileName: String) {
        try {
            file(dirUrl, fileName).delete()
        } catch (e: IOException) {
            throw FileException("Cannot delete file at URL '$dirUrl/$fileName. Cause: '${e.message}'", e)
        }
    }

    override fun truncate(dirUrl: String) {
        try {
            dirFiles(dirUrl).forEach { it.delete() }
        } catch (e: IOException) {
            throw FileException("Cannot truncate directory at URL '$dirUrl'. Cause: '${e.message}", e)
        }
    }

    override fun stat(dirUrl: String, fileName: String): FileEntry? {
        val fileUrl = "$dirUrl/$fileName"

        return try {
            file(dirUrl, fileName)
                    .takeIf { it.isFile }
                    ?.run { FileEntry(fileName, length(), lastModified()) }
        } catch (e: IOException) {
            throw FileException("Cannot check file status at URL '$fileUrl'. Cause: '${e.message}", e)
        }
    }

    private fun file(dirUrl: String, fileName: String) = aem.project.file("$dirUrl/$fileName")

    private fun dirFiles(dirUrl: String) = (aem.project.file(dirUrl).listFiles() ?: arrayOf()).filter { it.isFile }

    companion object {
        const val NAME = "path"
    }
}
