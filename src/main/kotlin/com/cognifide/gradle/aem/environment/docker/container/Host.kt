package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.docker.Container
import org.gradle.util.GFileUtils
import java.io.File

/**
 * Manage files on host OS related with specific Docker container.
 * Provides DSL for e.g creating directories for volumes and providing extra files shared via volumes.
 */
class Host(val container: Container) {

    private val logger = container.aem.logger

    private val docker = container.docker

    val rootDir = File(docker.environment.rootDir, container.name)

    private val fileResolver = FileResolver(container.aem, File(rootDir, "files"))

    /**
     * Get file under environment root directory
     */
    fun file(path: String) = File(rootDir, path)

    fun files(options: FileResolver.() -> Unit) {
        fileResolver.apply(options)
    }

    fun resolveFiles(): List<File> {
        logger.info("Resolving files for container '${container.name}'")
        val files = fileResolver.allFiles
        logger.info("Resolved files for container '${container.name}':\n${files.joinToString("\n")}")

        return files
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        file(path).apply {
            logger.info("Ensuring directory '$this' for container '${container.name}'")
            GFileUtils.mkdirs(this)
        }
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        file(path).apply {
            logger.info("Cleaning directory '$this' for container '${container.name}'")
            deleteRecursively(); GFileUtils.mkdirs(this)
        }
    }
}
