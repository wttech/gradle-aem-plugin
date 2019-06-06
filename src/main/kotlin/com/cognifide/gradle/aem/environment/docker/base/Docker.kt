package com.cognifide.gradle.aem.environment.docker.base

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder
import org.buildobjects.process.ProcResult
import org.buildobjects.process.TimeoutException
import org.gradle.process.internal.streams.SafeStreams

@Suppress("TooGenericExceptionCaught")
object Docker {

    fun exec(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder("docker")
                    .withNoTimeout()
                    .withOutputStream(SafeStreams.systemOut())
                    .withErrorStream(SafeStreams.systemErr())
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    fun execQuietly(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder("docker")
                    .ignoreExitStatus()
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    fun execString(options: ProcBuilder.() -> Unit): String {
        return try {
            ProcBuilder("docker")
                    .withNoTimeout()
                    .apply(options)
                    .run()
                    .outputString.trim()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    private fun composeException(e: Exception): DockerException {
        return when (e) {
            is ExternalProcessFailureException -> DockerException("Docker command process failure!" +
                    " Command: '${e.command}', error: '${e.stderr}', exit code: '${e.exitValue}'", e)
            is TimeoutException -> DockerException("Docker command timeout! Error: '${e.message}'", e)
            else -> DockerException("Docker command unknown failure. Error: '${e.message}'", e)
        }
    }
}