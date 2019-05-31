package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import java.io.File
import org.gradle.util.GFileUtils

class LocalFileTransfer(aem: AemExtension) : AbstractFileTransfer(aem) {

    override val name: String
        get() = NAME

    override fun handles(fileUrl: String): Boolean = true

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        GFileUtils.mkdirs(target.parentFile)
        file(dirUrl, fileName).copyTo(target)
    }

    override fun uploadTo(dirUrl: String, fileName: String, source: File) {
        val target = file(dirUrl, fileName)
        GFileUtils.mkdirs(target.parentFile)
        source.copyTo(target)
    }

    override fun list(dirUrl: String): List<FileEntry> {
        return dirFiles(dirUrl).map { FileEntry.of(it) }
    }

    override fun deleteFrom(dirUrl: String, fileName: String) {
        file(dirUrl, fileName).delete()
    }

    override fun truncate(dirUrl: String) {
        dirFiles(dirUrl).forEach { it.delete() }
    }

    private fun file(dirUrl: String, fileName: String) = aem.project.file("$dirUrl/$fileName")

    private fun dirFiles(dirUrl: String): Array<out File> = (aem.project.file(dirUrl).listFiles() ?: arrayOf())

    companion object {
        const val NAME = "local"
    }
}