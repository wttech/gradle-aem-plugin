package com.cognifide.gradle.aem.environment.docker.base

import java.io.File
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

object CygPath {

    fun calculate(file: File) = calculate(file.toString())

    fun calculate(path: String): String {
        return try {
            ProcBuilder("cygpath")
                    .withArg(path)
                    .run()
                    .outputString.trim()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Cannot calculate 'cygpath' for path: $path", e)
        }
    }
}