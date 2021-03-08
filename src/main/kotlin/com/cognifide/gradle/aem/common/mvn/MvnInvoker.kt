package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec
import java.io.File

class MvnInvoker(private val aem: AemExtension, private val forcedArgs: List<String> = listOf()) {

    @Internal
    val workingDir = aem.obj.dir()

    fun workingDir(dir: File) {
        workingDir.set(dir)
    }

    @Input
    val executableArgs = aem.obj.strings {
        set(aem.project.provider {
            when {
                OperatingSystem.current().isWindows -> listOf("cmd", "/c", "mvn")
                else -> listOf("mvn")
            }
        })
    }

    @Input
    val args = aem.obj.strings {
        set(listOf("-B"))
        aem.prop.string("mvn.execArgs")?.let { set(it.split(" ")) }
    }

    fun args(vararg values: String) = args(values.asIterable())

    fun args(values: Iterable<String>) {
        args.addAll(values)
    }

    private var specOptions: ExecSpec.() -> Unit = {}

    fun spec(options: ExecSpec.() -> Unit) {
        this.specOptions = options
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    fun invoke() {
        val dir = workingDir.get().asFile
        if (!dir.exists()) {
            throw MvnException("Cannot run Maven build at non-existing directory: '$workingDir'!")
        }
        val clArgs = executableArgs.get() + forcedArgs + args.get()
        try {
            aem.project.exec { spec ->
                spec.apply(specOptions)
                spec.workingDir(workingDir)
                spec.commandLine = clArgs
            }
        } catch (e: Exception) {
            throw MvnException(listOf(
                "Cannot invoke Maven properly!",
                "Directory: $dir",
                "Args: ${clArgs.joinToString(" ")}",
                "Cause: ${e.message}"
            ).joinToString("\n"), e)
        }
    }
}
