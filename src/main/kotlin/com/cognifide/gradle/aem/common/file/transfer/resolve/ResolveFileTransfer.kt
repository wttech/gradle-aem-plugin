package com.cognifide.gradle.aem.common.file.transfer.resolve

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import java.io.File

class ResolveFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    override val parallelable = false

    override val name = NAME

    override val protocols = listOf("$NAME://*")

    override fun handles(fileUrl: String): Boolean {
        return super.handles(fileUrl) || DependencyOptions.isNotation(fileUrl)
    }

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        resolve(dirUrl).apply {
            inputStream().use { downloader().download(length(), it, target) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun resolve(value: Any) = try {
        DependencyOptions.resolve(aem, when (value) {
            is String -> value.substringAfter("://")
            else -> value
        })
    } catch (e: Exception) {
        throw ResolveFileException("Cannot resolve value '$value'. Cause: ${e.message}", e)
    }

    companion object {
        const val NAME = "resolve"
    }
}
