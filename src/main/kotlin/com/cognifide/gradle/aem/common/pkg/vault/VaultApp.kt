package com.cognifide.gradle.aem.common.pkg.vault

import java.io.File
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.util.console.commands.CmdConsole
import org.gradle.api.Project

class VaultApp(val project: Project) : VaultFsApp() {

    companion object {
        const val CURRENT_WORKING_DIR = "user.dir"
    }

    private val executionContext by lazy { VaultExecutionContext(this).apply { installCommand(CmdConsole()) } }

    override fun getDefaultContext() = executionContext

    fun execute(command: String, workingPath: String) = execute(command.split(" "), workingPath)

    fun execute(command: String, workingDir: File) = execute(command, workingDir.absolutePath)

    fun execute(args: List<String>, workingDir: File) = execute(args, workingDir.absolutePath)

    /**
     * TODO This could be potentially improved by overriding few methods of base class
     */
    @Synchronized
    fun execute(args: List<String>, workingPath: String) {
        val cwd = System.getProperty(CURRENT_WORKING_DIR)
        try {
            System.setProperty(CURRENT_WORKING_DIR, workingPath)
            run(args.toTypedArray())
        } finally {
            System.setProperty(CURRENT_WORKING_DIR, cwd)
        }
    }
}
