package com.cognifide.gradle.aem.environment

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable

class DirectoryOptions(private val environment: Environment) : Serializable {

    val regulars = mutableListOf<File>()

    val caches = mutableListOf<File>()

    fun regular(vararg paths: String) = regular(paths.asIterable())

    fun regular(paths: Iterable<String>) {
        regularDirs(paths.map { File(environment.rootDir, it) })
    }

    fun regularDirs(files: Iterable<File>) {
        regulars.addAll(files)
    }

    fun cache(vararg paths: String) = cache(paths.asIterable())

    fun cache(paths: Iterable<String>) {
        cacheDirs(paths.map { File(environment.rootDir, it) })
    }

    fun cacheDirs(files: Iterable<File>) {
        caches.addAll(files)
    }

    @get:JsonIgnore
    val all: List<File>
        get() = regulars + caches
}