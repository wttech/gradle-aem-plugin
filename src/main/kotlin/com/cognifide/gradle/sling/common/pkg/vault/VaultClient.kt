package com.cognifide.gradle.sling.common.pkg.vault

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.cli.CliApp
import com.cognifide.gradle.common.utils.using
import org.apache.commons.lang3.time.StopWatch
import java.io.File

class VaultClient(val sling: SlingExtension) {

    private val cli = CliApp(sling).apply {
        dependencyNotation.apply {
            convention("org.apache.jackrabbit.vault:vault-cli:3.4.0:bin")
            sling.prop.string(("vault.cli.dependency"))?.let { set(it) }
        }
        executable.apply {
            convention("vault-cli-3.4.0/bin/vlt")
            sling.prop.string("vault.cli.executable")?.let { set(it) }
        }
    }

    fun cli(options: CliApp.() -> Unit) = cli.using(options)

    val command = sling.obj.string()

    val commandProperties = sling.obj.map<String, Any> { convention(mapOf("sling" to sling)) }

    val commandEffective get() = sling.prop.expand(command.get(), commandProperties.get())

    val contentDir = sling.obj.dir { convention(sling.packageOptions.contentDir) }

    val contentRelativePath = sling.obj.string()

    val contentDirEffective: File
        get() {
            var workingDir = contentDir.map { it.asFile.resolve(Package.JCR_ROOT) }.get()
            if (!contentRelativePath.orNull.isNullOrBlank()) {
                workingDir = workingDir.resolve(contentRelativePath.get())
            }

            return workingDir
        }

    fun run(): VaultSummary {
        if (commandEffective.isBlank()) {
            throw VaultException("Vault command cannot be blank.")
        }

        sling.logger.lifecycle("Working directory: $contentDirEffective")
        sling.logger.lifecycle("Executing command: vlt $commandEffective")

        val stopWatch = StopWatch().apply { start() }
        cli.exec(contentDirEffective, commandEffective)
        stopWatch.stop()

        return VaultSummary(commandEffective, contentDirEffective, stopWatch.time)
    }
}
