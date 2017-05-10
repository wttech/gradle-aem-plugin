package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemInstance
import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.util.console.ExecutionContext
import org.apache.jackrabbit.vault.util.console.commands.CmdConsole
import org.slf4j.LoggerFactory

class VltApp(val instance: AemInstance, val contentPath: String) : VaultFsApp() {

    companion object {
        private val LOG = LoggerFactory.getLogger(VltApp::class.java)
    }

    override fun getDefaultContext(): ExecutionContext {
        return defaultContext
    }

    private val defaultContext by lazy {
        val ctx = VltExecutionContext(this)
        ctx.installCommand(CmdConsole()); ctx
    }

    fun executeCommand(command: String, params: List<String> = listOf()): Unit {
        val allParams = mutableListOf<String>()
        allParams.addAll(listOf("--credentials", instance.user + ":" + instance.password))
        allParams.add(command)
        allParams.add("--force")
        allParams.addAll(params)
        allParams.addAll(listOf(instance.url, "/", contentPath))

        run(allParams.toTypedArray())
    }

    override fun prepare(cl: CommandLine) {
        LOG.info(cl.toString())

        super.prepare(cl)
    }
}
