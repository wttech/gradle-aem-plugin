package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.LocalInstance
import java.io.File
import java.io.IOException
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder
import org.buildobjects.process.ProcResult
import org.buildobjects.process.TimeoutException

class Script(val instance: LocalInstance, val shellCommand: List<String>, val wrapper: File, val bin: File) {

    val commandLine: List<String> get() = shellCommand + listOf(wrapper.absolutePath)

    val commandString: String get() = commandLine.joinToString(" ")

    val command: String get() = commandLine.first()

    val args: List<String> get() = commandLine.subList(1, commandLine.size)

    @Suppress("SpreadOperator", "TooGenericExceptionCaught")
    fun executeSync(options: ProcBuilder.() -> Unit = {}): ProcResult {
        return try {
            ProcBuilder(command, *args.toTypedArray())
                    .withWorkingDirectory(instance.dir)
                    .withTimeoutMillis(instance.localManager.scriptTimeout.get())
                    .ignoreExitStatus()
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw when (e) {
                is ExternalProcessFailureException -> InstanceException("Local instance script failure: $this! " +
                        "Error: '${e.stderr}', exit code: '${e.exitValue}'", e)
                is TimeoutException -> InstanceException("Local instance script timeout: $this! Cause: '${e.message}'", e)
                else -> InstanceException("Local instance script failure: $this! Cause: '${e.message}'", e)
            }
        }
    }

    fun executeAsync() {
        try {
            ProcessBuilder(commandLine).directory(instance.dir).start()
        } catch (e: IOException) {
            throw InstanceException("Local instance script failure: $this", e)
        }
    }

    override fun toString(): String = "Script(command=$commandString)"
}
