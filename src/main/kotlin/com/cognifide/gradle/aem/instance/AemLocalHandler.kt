package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.internal.FileOperations
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.GFileUtils
import java.io.File
import java.io.Serializable

/**
 * TODO Manage background AEM process properly (java-service-wrapper or commons-exec?)
 */
class AemLocalHandler(val base: AemInstance, val project: Project) {

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
        logger.info("Creating instance at path '${dir.absolutePath}'")

        val filesDir = File(config.instanceFilesPath)

        logger.info("Copying configured instance files from '${filesDir.absolutePath}'")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Copying resolved instance files: ${files.map { it.absolutePath }}")
        GFileUtils.mkdirs(dir)
        files.forEach { FileUtils.copyFileToDirectory(it, dir) }

        logger.info("Copying missing instance files (preserving defaults")
        FileOperations.copyResources(AemPlugin.INSTANCE_FILES_PATH, dir, true)

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.instanceFilesExpanded, { source ->
            PropertyParser(project).expand(source, properties)
        })

        logger.info("Created instance with success")
    }

    fun validate() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}")
        }
    }

    val properties: Map<String, Serializable>
        get() {
            return mapOf(
                    "instance" to base,
                    "jar" to jar,
                    "license" to license
            )
        }

    fun destroy() {
        logger.info("Destroying instance at path '${dir.absolutePath}'")

        if (dir.exists()) {
            dir.deleteRecursively()

        }

        logger.info("Destroyed instance with success")
    }


    // TODO Implement 'up'
    fun up() {
        Runtime.getRuntime().exec("java -jar ${jar.absolutePath}")
    }

    // TODO Implement 'down'
    fun down() {
        // ...
    }

    override fun toString(): String {
        return "AemLocalHandler(dir=${dir.absolutePath})"
    }

}