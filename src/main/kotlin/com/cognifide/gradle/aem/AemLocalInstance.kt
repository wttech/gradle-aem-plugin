package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.internal.Patterns
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * TODO download jar and license file using SMB or HTTP (share code used in aemSatisfy)
 * TODO generate start.bat or start.sh with debug ports
 * TODO put each dir into create:destroy outputs / task caching
 */
class AemLocalInstance(val base: AemInstance, val project: Project) {

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val dir = File("${config.instancesPath}/${base.name}")

    val jar: File by lazy {
        var result: File? = null
        val files = dir.listFiles({ file, _ -> Patterns.wildcard(file, "cq-quickstart*.jar") })
        if (files != null) {
            result = files.firstOrNull()
        }

        result ?: File(dir, "cq-quickstart.jar")
    }

    val license = File(dir, "license.properties")

    fun create() {
        logger.info("JAR file found: ${jar.absolutePath}, exists: ${jar.exists()}")
        logger.info("License file found: ${license.absolutePath}, exists: ${jar.exists()}")

        // TODO generate start.bat, start.sh with corresponding debug port from base.debugPort
    }

    fun destroy() {
        dir.deleteRecursively()
    }

    override fun toString(): String {
        return "AemLocalInstance(dir=${dir.absolutePath})"
    }

}