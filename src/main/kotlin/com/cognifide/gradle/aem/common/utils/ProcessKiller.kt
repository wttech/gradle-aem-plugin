package com.cognifide.gradle.aem.common.utils

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.LocalInstanceException
import org.buildobjects.process.ProcBuilder
import org.gradle.internal.os.OperatingSystem
import java.util.concurrent.TimeUnit

class ProcessKiller(private val aem: AemExtension) {

    private var options: ProcBuilder.() -> Unit = {}

    fun options(options: ProcBuilder.() -> Unit) {
        this.options = options
    }

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
                    .withWorkingDirectory(aem.project.projectDir)
                    .withTimeoutMillis(TimeUnit.SECONDS.toMillis(30))
                    .withExpectedExitStatuses(0)
                    .apply(this.options)
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw AemException("Instance killing failed for PID '$pid'! Cause: ${e.message}", e)
        }
    }
}