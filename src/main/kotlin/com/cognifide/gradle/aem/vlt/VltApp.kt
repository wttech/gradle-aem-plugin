package com.cognifide.gradle.aem.vlt

import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.gradle.api.logging.Logger

class VltApp(val logger: Logger) : VaultFsApp() {

    override fun prepare(cl: CommandLine) {
        logger.info("Executing Vault application: $cl")

        super.prepare(cl)
    }

    fun execute(command: String): Unit {
        execute(command.split(" "))
    }

    fun execute(args: List<String>): Unit {
        run(args.toTypedArray())
    }

}
