package com.cognifide.gradle.aem.common.utils

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import org.buildobjects.process.ProcBuilder
import org.gradle.internal.os.OperatingSystem
import java.util.concurrent.TimeUnit

class WebBrowser(private val aem: AemExtension) {

    private var options: ProcBuilder.() -> Unit = {}

    fun options(options: ProcBuilder.() -> Unit) {
        this.options = options
    }

    @Suppress("TooGenericExceptionCaught", "MagicNumber")
    fun open(url: String, options: ProcBuilder.() -> Unit = {}) {
        try {
            val os = OperatingSystem.current()
            val command = when {
                os.isWindows -> "explorer"
                os.isMacOsX -> "open"
                else -> "sensible-browser"
            }

            ProcBuilder(command, url)
                    .withWorkingDirectory(aem.project.projectDir)
                    .withTimeoutMillis(TimeUnit.SECONDS.toMillis(30))
                    .ignoreExitStatus()
                    .apply(this.options)
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw AemException("Browser opening command failed! Cause: ${e.message}", e)
        }
    }
}
