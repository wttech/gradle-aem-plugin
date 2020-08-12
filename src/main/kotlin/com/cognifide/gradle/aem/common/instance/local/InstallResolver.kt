package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.using

class InstallResolver(private val aem: AemExtension) {

    private val common = aem.common

    private val fileResolver = FileResolver(common).apply {
        downloadDir.convention(aem.obj.buildDir("localInstance/install"))
        aem.prop.file("localInstance.install.downloadDir")?.let { downloadDir.set(it) }
        aem.prop.list("localInstance.install.urls")?.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")

            group("cmd.$no.$fileName") { get(url) }
        }
    }

    fun files(configurer: FileResolver.() -> Unit) = fileResolver.using(configurer)

    fun files(vararg values: Any) = fileResolver.getAll(values.asIterable())

    fun files(values: Iterable<Any>) = fileResolver.getAll(values)

    val files get() = fileResolver.files
}
