package com.cognifide.gradle.aem.common.cli

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.provider.Provider
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

open class CliApp(protected val aem: AemExtension) {

    val dependencyNotation = aem.obj.string()

    val dependencyDir = aem.obj.dir()

    val executable = aem.obj.string()

    fun exec(workingDir: File, command: String) = exec {
        workingDir(workingDir)
        args(command.split(" "))
    }

    fun exec(options: ExecSpec.() -> Unit = {}): ExecResult {
        extractArchive()

        val executableFile = dependencyDir.get().asFile.resolve(executable.get())
        if (!executableFile.exists()) {
            throw CliException("Executable file '${executable.get()}' of CLI application '${dependencyNotation.get()}'" +
                    " cannot be found at path '$executableFile' after extracting archive!")
        }

        return aem.project.exec {
            it.apply {
                standardInput = SafeStreams.emptyInput()
                standardOutput = SafeStreams.systemOut()
                errorOutput = SafeStreams.systemErr()
                options()
                setExecutable(executableFile)
            }
        }
    }

    private fun downloadArchive(dependency: Provider<String>): Provider<File> = dependency.map { notation ->
        aem.project.configurations.detachedConfiguration(aem.project.dependencies.create(notation)).apply {
            isTransitive = false
        }.singleFile
    }

    private fun extractArchive() {
        if (!dependencyDir.get().asFile.exists()) {
            val file = downloadArchive(dependencyNotation)
            val fileTree = when (file.get().extension) {
                "zip" -> aem.project.zipTree(file)
                else -> aem.project.tarTree(file)
            }
            fileTree.visit { it.copyTo(it.relativePath.getFile(dependencyDir.get().asFile)) }
        }
    }
}
