package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.internal.FileOperations
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.GFileUtils
import java.io.File

/**
 * TODO put each dir into create:destroy outputs / task caching
 */
class AemLocalInstance(val base: AemInstance, val project: Project) {

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val dir = File("${config.instancesPath}/${base.name}")

    val jar: File by lazy {
        var result: File? = null
        val files = dir.listFiles({ _, name -> Patterns.wildcard(name, "cq-quickstart*.jar") })
        if (files != null) {
            result = files.firstOrNull()
        }

        result ?: File(dir, "cq-quickstart.jar")
    }

    val license = File(dir, "license.properties")

    fun create(files: List<File>) {
        logger.info("Copying instance files to '${dir.absolutePath}'")
        GFileUtils.mkdirs(dir)
        files.forEach { FileUtils.copyFileToDirectory(it, dir) }

        logger.info("JAR file found: ${jar.absolutePath}, exists: ${jar.exists()}")
        logger.info("License file found: ${license.absolutePath}, exists: ${jar.exists()}")

        FileOperations.copyResources("local-instance", dir, false, { file, input ->
            if (Patterns.wildcard(file, listOf("**/*.bat", "**/*.sh"))) {
                val text = input.bufferedReader().use { it.readText() }

                PropertyParser(project).expand(text, mapOf(
                        "instance" to base,
                        "jar" to jar,
                        "license" to license
                )).byteInputStream()
            } else {
                input
            }
        })
    }

    fun destroy() {
        dir.deleteRecursively()
    }

    fun up() {
        Runtime.getRuntime().exec("java -jar ${jar.absolutePath}")
    }

    fun down() {
        // TODO ...
    }

    override fun toString(): String {
        return "AemLocalInstance(dir=${dir.absolutePath})"
    }

}