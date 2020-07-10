package com.cognifide.gradle.sling.common.cli

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.provider.Provider
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

open class CliApp(protected val sling: SlingExtension) {

    val dependencyNotation = sling.obj.string()

    val dependencyExtension = sling.obj.boolean { convention(true) }

    val dependencyDir = sling.obj.dir {
        convention(sling.project.rootProject.layout.projectDirectory.dir(dependencyNotation.map {
            ".gradle/sling/cli/${Formats.toHashCodeHex(it)}"
        }))
    }

    val executable = sling.obj.string()

    val executableExtension = sling.obj.boolean { convention(true) }

    fun exec(workingDir: File, command: String) = exec {
        workingDir(workingDir)
        args(command.split(" "))
    }

    @Suppress("TooGenericExceptionCaught")
    fun exec(options: ExecSpec.() -> Unit = {}): ExecResult {
        extractArchive()

        val executablePath = executable.map { if (executableExtension.get()) "$it${sling.commonOptions.executableExtension.get()}" else it }
        val executableFile = dependencyDir.get().asFile.resolve(executablePath.get())
        if (!executableFile.exists()) {
            throw CliException("CLI application '${dependencyNotation.get()}' executable file '${executablePath.get()}'" +
                    " cannot be found at path '$executableFile' after extracting archive!")
        }

        return try {
            sling.project.exec {
                it.apply {
                    standardInput = SafeStreams.emptyInput()
                    standardOutput = SafeStreams.systemOut()
                    errorOutput = SafeStreams.systemErr()
                    options()
                    setExecutable(executableFile)
                }
            }
        } catch (e: Exception) {
            throw CliException("CLI application '${dependencyNotation.get()}' cannot be executed properly! Cause: ${e.message}", e)
        }
    }

    private fun downloadArchive(dependency: Provider<String>): Provider<File> = dependency.map { notation ->
        sling.project.configurations.detachedConfiguration(sling.project.dependencies.create(notation)).apply {
            isTransitive = false
        }.singleFile
    }

    private fun extractArchive() = sling.common.buildScope.doOnce("extracting archive '${dependencyNotation.get()}'") {
        if (dependencyDir.get().asFile.exists()) {
            return@doOnce
        }

        val file = downloadArchive(dependencyNotation.map {
            if (dependencyExtension.get()) "$it@${sling.commonOptions.archiveExtension.get()}" else it
        })
        val fileTree = when (file.get().extension) {
            "zip" -> sling.project.zipTree(file)
            else -> sling.project.tarTree(file)
        }
        fileTree.visit { it.copyTo(it.relativePath.getFile(dependencyDir.get().asFile)) }
    }
}
