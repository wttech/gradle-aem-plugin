package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.util.console.ExecutionContext
import org.apache.jackrabbit.vault.util.console.commands.CmdConsole
import org.gradle.api.Project
import java.io.File

class VltApp(val project: Project) : VaultFsApp() {

    companion object {
        const val CURRENT_WORKING_DIR = "user.dir"
    }

    private val logger = project.logger

    private val config = AemConfig.of(project)

    private val executionContext by lazy {
        val result = VltExecutionContext(this)
        result.installCommand(CmdConsole())
        result
    }

    override fun getDefaultContext(): ExecutionContext {
        return executionContext
    }

    override fun prepare(command: CommandLine) {
        logger.info("Working directory: $workingDir")
        logger.info("Executing: vlt $command")

        super.prepare(command)
    }

    fun execute(command: String): Unit {
        execute(command.split(" "))
    }

    fun execute(args: List<String>): Unit {
        execute(args, workingDir.absolutePath)
    }

    val workingDir: File
        get() {
            var path = "${config.contentPath}/${PackagePlugin.JCR_ROOT}"

            val relativePath = project.properties["aem.vlt.path"] as String?
            if (!relativePath.isNullOrBlank()) {
                path = "$path/$relativePath"
            }

            return File(path)
        }

    /**
     * TODO This could be potentially improved by overriding few methods of base class
     * @see VaultFsApp.init
     */
    @Synchronized
    private fun execute(args: List<String>, workingPath: String) {
        val cwd = System.getProperty(CURRENT_WORKING_DIR)

        System.setProperty(CURRENT_WORKING_DIR, workingPath)
        run(args.toTypedArray())
        System.setProperty(CURRENT_WORKING_DIR, cwd)
    }
}
