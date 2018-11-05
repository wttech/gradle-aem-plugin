package com.cognifide.gradle.aem.internal.file.resolver

import java.io.File

open class FileResolution(val group: FileGroup, val id: String, private val action: (FileResolution) -> File) {

    val dir = File("${group.downloadDir}/$id")

    val file: File by lazy { process(action(this)) }

    protected open fun process(file: File): File = file

}