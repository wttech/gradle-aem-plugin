package com.cognifide.gradle.aem.common.cli

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.build.DependencyFile
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

open class JarApp(val aem: AemExtension) {

    val dependencyNotation = aem.obj.string()

    fun exec(workingDir: File, command: String) = exec(workingDir, command.split(" "))

    fun exec(workingDir: File, args: Iterable<Any>) = exec {
        workingDir(workingDir)
        args(listOf(jar) + args)
    }

    fun exec(vararg args: Any) = exec(args.asIterable())

    fun exec(args: Iterable<Any>) = exec { args(listOf(jar) + args) }

    @Suppress("TooGenericExceptionCaught")
    fun exec(options: JavaExecSpec.() -> Unit = {}): ExecResult {
        return try {
            aem.project.javaexec {
                it.apply {
                    executable(aem.commonOptions.javaSupport.launcherPath)
                    standardInput = SafeStreams.emptyInput()
                    standardOutput = SafeStreams.systemOut()
                    errorOutput = SafeStreams.systemErr()
                    main = "-jar"
                    options()
                }
            }
        } catch (e: Exception) {
            throw JarException("Jar application '${dependencyNotation.get()}' cannot be executed properly! Cause: ${e.message}", e)
        }
    }

    val jar: File get() = DependencyFile(aem.project, dependencyNotation.get()).file
}
