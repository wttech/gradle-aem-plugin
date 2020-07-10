package com.cognifide.gradle.sling.common.instance.local

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.using

class InstallResolver(private val sling: SlingExtension) {

    private val common = sling.common

    private val fileResolver = FileResolver(common).apply {
        downloadDir.convention(sling.obj.buildDir("localInstance/install"))
        sling.prop.file("localInstance.install.downloadDir")?.let { downloadDir.set(it) }
        sling.prop.list("localInstance.install.urls")?.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")

            group("cmd.$no.$fileName") { get(url) }
        }
    }

    fun files(configurer: FileResolver.() -> Unit) = fileResolver.using(configurer)

    fun files(vararg values: Any) = fileResolver.getAll(values)

    fun files(values: Iterable<Any>) = fileResolver.getAll(values)

    val files get() = fileResolver.files
}
