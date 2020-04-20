package com.cognifide.gradle.aem.common.cli

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.provider.Provider
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

open class CliApp(protected val aem: AemExtension) {

    val dependencyNotation = aem.obj.string()

    val dependencyExtension = aem.obj.boolean { convention(true) }

    val dependencyDir = aem.obj.dir {
        convention(aem.project.rootProject.layout.buildDirectory.dir(dependencyNotation.map {
            "aem/cli/${Formats.toHashCodeHex(it)}"
        }))
    }

    val executable = aem.obj.string()

    val executableExtension = aem.obj.boolean { convention(true) }

    fun exec(workingDir: File, command: String) = exec {
        workingDir(workingDir)
        args(command.split(" "))
    }

    @Suppress("TooGenericExceptionCaught")
    fun exec(options: ExecSpec.() -> Unit = {}): ExecResult {
        extractArchive()

        val executablePath = executable.map { if (executableExtension.get()) "$it${aem.commonOptions.executableExtension.get()}" else it }
        val executableFile = dependencyDir.get().asFile.resolve(executablePath.get())
        if (!executableFile.exists()) {
            throw CliException("CLI application '${dependencyNotation.get()}' executable file '${executablePath.get()}'" +
                    " cannot be found at path '$executableFile' after extracting archive!")
        }

        return try {
            aem.project.exec {
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
        aem.project.configurations.detachedConfiguration(aem.project.dependencies.create(notation)).apply {
            isTransitive = false
        }.singleFile
    }

    private fun extractArchive() = aem.common.buildScope.doOnce("extracting archive '${dependencyNotation.get()}'") {
        if (dependencyDir.get().asFile.exists()) {
            return@doOnce
        }

        val file = downloadArchive(dependencyNotation.map {
            if (dependencyExtension.get()) "$it@${aem.commonOptions.archiveExtension.get()}" else it
        })
        val fileTree = when (file.get().extension) {
            "zip" -> aem.project.zipTree(file)
            else -> aem.project.tarTree(file)
        }
        fileTree.visit { it.copyTo(it.relativePath.getFile(dependencyDir.get().asFile)) }
    }
}
