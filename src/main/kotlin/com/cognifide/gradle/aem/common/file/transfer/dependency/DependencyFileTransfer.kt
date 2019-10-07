package com.cognifide.gradle.aem.common.file.transfer.dependency

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import java.io.File

class DependencyFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    private val configurations = aem.project.configurations

    private val dependencies = aem.project.dependencies

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("dependency://*")

    @Suppress("TooGenericExceptionCaught")
    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        val notation = fileName

        try {
            configurations.detachedConfiguration(dependencies.create(notation)).singleFile.apply {
                inputStream().use { downloader().download(length(), it, target) }
            }
        } catch (e: Exception) {
            throw DependencyFileException("Cannot resolve dependency '$notation' to file '$target'. Cause: ${e.message}", e)
        }
    }

    companion object {
        const val NAME = "dependency"
    }
}
