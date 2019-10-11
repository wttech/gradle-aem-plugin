package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import org.gradle.util.GFileUtils
import java.io.File

/**
 * File manager for host OS files related specific Docker container.
 * Provides DSL for e.g creating directories for volumes and providing extra files shared via volumes.
 */
class ContainerHost(val container: Container) {

    private val aem = container.aem

    private val logger = aem.logger

    private val docker = container.docker

    val rootDir = File(docker.environment.rootDir, container.name)

    var fileDir = File(rootDir, aem.props.string("environment.container.host.fileDir") ?: "files")

    fun file(path: String) = File(rootDir, path)

    fun resolveFiles(options: FileResolver.() -> Unit): List<File> {
        logger.info("Resolving files for container '${container.name}'")
        val files = FileResolver(container.aem, fileDir).apply(options).allFiles
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
