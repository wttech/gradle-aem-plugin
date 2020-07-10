package com.cognifide.gradle.sling.common.utils

import com.cognifide.gradle.sling.SlingException
import com.cognifide.gradle.sling.SlingExtension
import org.buildobjects.process.ProcBuilder
import org.gradle.internal.os.OperatingSystem
import java.util.concurrent.TimeUnit

class WebBrowser(private val sling: SlingExtension) {

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
                    .withWorkingDirectory(sling.project.projectDir)
                    .withTimeoutMillis(TimeUnit.SECONDS.toMillis(30))
                    .ignoreExitStatus()
                    .apply(this.options)
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw SlingException("Browser opening command failed! Cause: ${e.message}", e)
        }
    }
}
