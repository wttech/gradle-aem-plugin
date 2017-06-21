package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.util.console.ExecutionContext
import org.apache.jackrabbit.vault.util.console.commands.CmdConsole
import org.gradle.api.Project
import java.io.File

class VltApp(val instance: AemInstance, val project: Project) : VaultFsApp() {

    companion object {

        fun checkout(project: Project) {
            val config = AemConfig.of(project)
            val contentDir = File(config.contentPath)

            if (!contentDir.exists()) {
                project.logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
            }

            val instance = AemInstance.filter(project, config, AemInstance.FILTER_AUTHOR).first()
            val vltApp = VltApp(instance, project)
            val cmdArgs = listOf(instance.url, "/") + config.vaultCheckoutArgs + contentDir.absolutePath

            var filter = File(config.vaultFilterPath)
            val cmdFilterPath = project.properties["aem.vlt.checkout.filterPath"] as String?
            if (!cmdFilterPath.isNullOrBlank()) {
                val cmdFilter = project.file(cmdFilterPath)
                if (!cmdFilter.exists()) {
                    throw VltException("Vault check out filter file does not exist at path: ${cmdFilter.absolutePath}")
                }

                filter = cmdFilter
            }

            if (filter.exists()) {
                vltApp.executeCommand(listOf(CheckoutTask.COMMAND, "-f", filter.absolutePath) + cmdArgs)
            } else {
                vltApp.executeCommand(listOf(CheckoutTask.COMMAND) + cmdArgs)
            }
        }
    }

    init {
        configureLogger()
    }

    override fun getDefaultContext(): ExecutionContext {
        return defaultContext
    }

    private val defaultContext by lazy {
        val ctx = VltExecutionContext(this)
        ctx.installCommand(CmdConsole()); ctx
    }

    fun executeCommand(args: List<String>): Unit {
        val allArgs = mutableListOf<String>()
        allArgs.addAll(listOf("--credentials", instance.user + ":" + instance.password))
        allArgs.addAll(args)

        run(allArgs.toTypedArray())
    }

    override fun prepare(cl: CommandLine) {
        log.info("Executing Vault application: $cl")

        super.prepare(cl)
    }

    /**
     * TODO Try to hide VLT output when 'gradle -i' is not specified, below is not working...
     */
    private fun configureLogger() {
        log = project.logger
        /*
        val logLevel = project.logging.standardOutputCaptureLevel
        if (logLevel != null) {
            setLogLevel(logLevel.name)
        }
        */
    }
}
