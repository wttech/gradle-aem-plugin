package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.DockerException
import org.buildobjects.process.ProcBuilder

class Toolbox(aem: AemExtension) : Base(aem) {

    override val name: String
        get() = NAME

    override val hostIp: String
        get() = aem.props.string("environment.docker.toolbox.hostIp") ?: "192.168.99.100"

    override val safeVolumes: Boolean = true

    var cygpathPath = aem.props.string("environment.cygpath.path")
            ?: "C:\\Program Files\\Git\\usr\\bin\\cygpath.exe"

    override fun determinePath(path: String): String {
        return try {
            executeCygpath(path)
        } catch (e: DockerException) {
            aem.logger.debug("Cannot determine Docker path for '$path' using 'cygpath', because it is not available.", e)
            imitateCygpath(path)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun executeCygpath(path: String) = try {
        ProcBuilder(cygpathPath)
                .withArg(path)
                .run()
                .outputString.trim()
    } catch (e: Exception) {
        throw DockerException("Cannot execute '$cygpathPath' for path: $path", e)
    }

    fun imitateCygpath(path: String): String {
        return Regex("(\\w):/(.*)").matchEntire(path.replace("\\", "/"))?.let {
            val (letter, drivePath) = it.groupValues.drop(1)
            "/${letter.toLowerCase()}/$drivePath"
        } ?: path
    }

    companion object {
        const val NAME = "toolbox"
    }
}
