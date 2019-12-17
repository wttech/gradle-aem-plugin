package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.docker.Container
import java.io.File

/**
 * File manager for host OS files related specific Docker container.
 * Provides DSL for e.g creating directories for volumes and providing extra files shared via volumes.
 */
class HostFileManager(val container: Container) {

    private val aem = container.aem

    private val logger = aem.logger

    private val docker = container.docker

    val rootDir = File(docker.environment.rootDir, container.name)

    var fileDir = File(rootDir, aem.prop.string("environment.container.host.fileDir") ?: "files")

    fun file(path: String) = File(rootDir, path)

    val configDir = File(docker.environment.configDir, container.name)

    fun configFile(path: String) = File(configDir, path)

    fun resolveFiles(options: FileResolver.() -> Unit): List<File> {
        logger.info("Resolving files for container '${container.name}'")
        val files = aem.resolveFiles(fileDir, options)
        logger.info("Resolved files for container '${container.name}':\n${files.joinToString("\n")}")

        return files
    }

    fun ensureDir() {
        rootDir.apply {
            logger.info("Ensuring root directory '$this' for container '${container.name}'")
            mkdirs()
        }
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        file(path).apply {
            logger.info("Ensuring directory '$this' for container '${container.name}'")
            mkdirs()
        }
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        file(path).apply {
            logger.info("Cleaning directory '$this' for container '${container.name}'")
            deleteRecursively(); mkdirs()
        }
    }
}
