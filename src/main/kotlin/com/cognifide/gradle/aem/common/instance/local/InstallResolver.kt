package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class InstallResolver(private val aem: AemExtension) {

    private val common = aem.common

    var downloadDir = aem.prop.file("localInstance.install.downloadDir")
            ?: common.temporaryFile("instance/install")

    private val fileResolver = FileResolver(common, downloadDir)

    fun files(configurer: FileResolver.() -> Unit) {
        fileResolver.apply(configurer)
    }

    @get:JsonIgnore
    val files: List<File>
        get() = fileResolver.allFiles

    init {
        val urls = aem.prop.list("localInstance.install.urls") ?: listOf()
        urls.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")
            fileResolver.group("cmd.$no.$fileName") { get(url) }
        }
    }
}
