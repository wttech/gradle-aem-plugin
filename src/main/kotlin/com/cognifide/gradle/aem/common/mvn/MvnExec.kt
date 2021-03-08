package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.aem
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

open class MvnExec : DefaultTask() {

    val aem = project.aem

    val workingDir = aem.obj.dir()

    val executableArgs = aem.obj.strings {
        set(project.provider {
            when {
                OperatingSystem.current().isWindows -> listOf("cmd", "/c", "mvn")
                else -> listOf("mvn")
            }
        })
    }

    val forcedArgs get() = listOf("-N")

    val args = aem.obj.strings {
        set(listOf())
        aem.prop.string("mvn.execArgs")?.split(" ") ?: listOf("-B")
    }

    fun args(vararg values: String) = args(values.asIterable())

    fun args(values: Iterable<String>) {
        args.addAll(values)
    }

    private var specOptions: ExecSpec.() -> Unit = {}

    fun spec(options: ExecSpec.() -> Unit) {
        this.specOptions = options
    }

    @TaskAction
    fun exec() {
        if (!workingDir.get().asFile.exists()) {
            throw MvnException("Cannot run Maven build at non-existing directory: '$workingDir'!")
        }
        project.exec { spec ->
            spec.apply(specOptions)
            spec.workingDir(workingDir)
            spec.commandLine = executableArgs.get() + forcedArgs + args.get()
        }
    }
}
