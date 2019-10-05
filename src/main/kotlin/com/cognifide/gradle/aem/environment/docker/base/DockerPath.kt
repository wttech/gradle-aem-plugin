package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.docker.base.runtime.Toolbox
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder
import java.io.File

class DockerPath(private val environment: Environment) {

    private val aem = environment.aem

    var cygpathPath = aem.props.string("environment.cygpath.path")
            ?: "C:\\Program Files\\Git\\usr\\bin\\cygpath.exe"

    fun get(file: File) = get(file.toString())

    fun get(path: String) = when (environment.dockerRuntime) {
        is Toolbox -> try {
            executeCygpath(path)
        } catch (e: DockerException) {
            aem.logger.debug("Cannot determine Docker path for '$path' using 'cygpath', because it is not available.", e)
            imitateCygpath(path)
        }
        else -> Formats.normalizePath(path)
    }

    fun executeCygpath(path: String) = try {
        ProcBuilder(cygpathPath)
                .withArg(path)
                .run()
                .outputString.trim()
    } catch (e: ExternalProcessFailureException) {
        throw DockerException("Cannot execute 'cygpath' for path: $path", e)
    }

    fun imitateCygpath(path: String): String {
        return Regex("(\\w):/(.*)").matchEntire(path.replace("\\", "/"))?.let {
            val (letter, drivePath) = it.groupValues.drop(1)
            "/${letter.toLowerCase()}/$drivePath"
        } ?: path
    }
}
