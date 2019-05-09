package com.cognifide.gradle.aem.environment.docker.base

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder
import org.buildobjects.process.ProcResult
import org.gradle.process.internal.streams.SafeStreams

object Docker {

    fun exec(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder("docker")
                    .withNoTimeout()
                    .withOutputStream(SafeStreams.systemOut())
                    .withErrorStream(SafeStreams.systemErr())
                    .apply(options)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw composeException(e)
        }
    }

    fun execQuietly(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder("docker")
                    .ignoreExitStatus()
                    .apply(options)
                    .run()
        } catch (e: ExternalProcessFailureException) {
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
        } catch (e: ExternalProcessFailureException) {
            throw composeException(e)
        }
    }

    private fun composeException(e: ExternalProcessFailureException): DockerException {
        return DockerException("Cannot execute Docker command: '${e.command}'! Error: ${e.stderr}. Exit code: ${e.exitValue}")
    }
}