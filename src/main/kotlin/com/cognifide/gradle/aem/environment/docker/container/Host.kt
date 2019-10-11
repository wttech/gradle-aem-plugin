package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.environment.docker.Container
import org.gradle.util.GFileUtils

/**
 * DSL for creating directories on host machine for volumes related with container.
 */
class Host(val container: Container) {

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        container.file(path).apply { GFileUtils.mkdirs(this) }
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        container.file(path).apply { deleteRecursively(); GFileUtils.mkdirs(this) }
    }

}
