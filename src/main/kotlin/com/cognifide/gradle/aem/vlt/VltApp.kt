package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.gradle.api.Project
import java.io.File

class VltApp(val project: Project) : VaultFsApp() {

    companion object {
        const val CURRENT_WORKING_DIR = "user.dir"
    }

    val logger = project.logger

    val config = AemConfig.of(project)

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
            var path = "${config.contentPath}/${AemPlugin.JCR_ROOT}"

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
