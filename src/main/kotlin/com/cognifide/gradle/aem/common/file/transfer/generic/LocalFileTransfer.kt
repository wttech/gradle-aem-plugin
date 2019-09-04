package com.cognifide.gradle.aem.common.file.transfer.generic

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import java.io.File
import java.io.IOException

/**
 * File transfer which is using files directly instead of copying.
 */
class LocalFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("local://*")

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        if (!target.exists()) {
            throw FileException("Cannot use local file '$target' because it does not exist.")
        }
    }

    override fun list(dirUrl: String): List<FileEntry> = try {
        dirFiles(dirUrl).map { FileEntry.of(it) }
    } catch (e: IOException) {
        throw FileException("Cannot list local files in directory '$dirUrl'. Cause: '${e.message}'", e)
    }

    private fun dirFiles(dirUrl: String) = (aem.project.file(dirUrl).listFiles() ?: arrayOf()).filter { it.isFile }

    override fun stat(dirUrl: String, fileName: String): FileEntry? {
        val fileUrl = "$dirUrl/$fileName"

        return try {
            file(dirUrl, fileName)
                    .takeIf { it.isFile }
                    ?.run { FileEntry(fileName, length(), lastModified()) }
        } catch (e: IOException) {
            throw FileException("Cannot check local file status '$fileUrl'. Cause: '${e.message}", e)
        }
    }

    private fun file(dirUrl: String, fileName: String) = aem.project.file("$dirUrl/$fileName")

    companion object {
        const val NAME = "local"
    }
}
