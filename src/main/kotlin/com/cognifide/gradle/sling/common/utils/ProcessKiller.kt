package com.cognifide.gradle.sling.common.utils

import com.cognifide.gradle.sling.SlingException
import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.LocalInstanceException
import org.buildobjects.process.ProcBuilder
import org.gradle.internal.os.OperatingSystem
import java.util.concurrent.TimeUnit

class ProcessKiller(private val sling: SlingExtension) {

    private var options: ProcBuilder.() -> Unit = {}

    fun options(options: ProcBuilder.() -> Unit) {
        this.options = options
    }

    @Suppress("TooGenericExceptionCaught", "SpreadOperator", "MagicNumber")
    fun kill(pid: Int, options: ProcBuilder.() -> Unit = {}) {
        try {
            val os = OperatingSystem.current()
            val command = when {
                os.isWindows -> "taskkill /F /PID"
                os.isUnix -> "kill -9"
                else -> throw LocalInstanceException("Instance killing is not supported on current OS ($os)!")
            }

            val allArgs = command.split(" ")
            val executable = allArgs.first()
            val args = allArgs.drop(1) + pid.toString()

            ProcBuilder(executable, *args.toTypedArray())
                    .withWorkingDirectory(sling.project.projectDir)
                    .withTimeoutMillis(TimeUnit.SECONDS.toMillis(10))
                    .withExpectedExitStatuses(0)
                    .apply(this.options)
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw SlingException("Instance killing failed for PID '$pid'! Cause: ${e.message}", e)
        }
    }
}
