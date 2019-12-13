package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class InstallResolver(private val aem: AemExtension) {

    var downloadDir = aem.prop.string("localInstance.install.downloadDir")?.let { aem.project.file(it) }
            ?: AemTask.temporaryDir(aem.project, "instance/install")

    private val fileResolver = FileResolver(aem, downloadDir)

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
