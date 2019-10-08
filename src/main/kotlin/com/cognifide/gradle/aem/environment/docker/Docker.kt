package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.util.GFileUtils
import java.io.File

class Docker(val environment: Environment) {

    private val aem = environment.aem

    @get:JsonIgnore
    val running: Boolean
        get() = stack.running && httpd.running

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    @JsonIgnore
    val stack = Stack(environment)

    val containers = mutableListOf<Container>()

    /**
     * Represents Docker container named 'aem_httpd' and provides API for manipulating it.
     */
    @JsonIgnore
    val httpd = Container(environment, "aem_httpd") // TODO do not hardcode it

    val runtime: Runtime = Runtime.determine(aem)

    val composeFile
        get() = File(environment.rootDir, "docker-compose.yml")

    val composeSourceFile: File
        get() = File(environment.configDir, "docker-compose.yml.peb")

    /**
     * Generator for paths in format expected in 'docker-compose.yml' files.
     */
    val pathGenerator = PathGenerator(environment)

    val configPath: String
        get() = pathGenerator.get(environment.configDir)

    val rootPath: String
        get() = pathGenerator.get(environment.rootDir)


    fun init() {
        syncComposeFile()
    }

    private fun syncComposeFile() {
        aem.logger.info("Synchronizing Docker compose file: $composeSourceFile -> $composeFile")

        if (!composeSourceFile.exists()) {
            throw EnvironmentException("Docker compose file does not exist: $composeSourceFile")
        }

        GFileUtils.deleteFileQuietly(composeFile)
        GFileUtils.copyFile(composeSourceFile, composeFile)
        aem.props.expand(composeFile, mapOf("environment" to this))
    }

    fun up() {
        stack.reset()
        httpd.deploy()
    }

    fun clean() {
        /*
               directories.caches.forEach { dir ->
            if (dir.exists()) {
                aem.logger.info("Cleaning AEM environment cache directory: $dir")
                FileOperations.removeDirContents(dir)
            }
        }
         */
    }

    fun down() {
        stack.undeploy()
    }

}
