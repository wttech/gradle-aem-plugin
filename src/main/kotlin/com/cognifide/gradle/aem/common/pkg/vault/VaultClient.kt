package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.cli.CliApp
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.utils.using
import java.io.File
import kotlin.system.measureTimeMillis

class VaultClient(val aem: AemExtension) {

    private val cli = CliApp(aem).apply {
        dependencyNotation.apply {
            convention("org.apache.jackrabbit.vault:vault-cli:3.4.0:bin")
            aem.prop.string(("vault.cli.dependency"))?.let { set(it) }
        }
        executable.apply {
            convention("vault-cli-3.4.0/bin/vlt")
            aem.prop.string("vault.cli.executable")?.let { set(it) }
        }
    }

    fun cli(options: CliApp.() -> Unit) = cli.using(options)

    val command = aem.obj.string()

    val commandProperties = aem.obj.map<String, Any> { convention(mapOf("aem" to aem)) }

    val commandEffective get() = aem.prop.expand(command.get(), commandProperties.get())

    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    val contentRelativePath = aem.obj.string()

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

        aem.logger.lifecycle("Working directory: $contentDirEffective")
        aem.logger.lifecycle("Executing command: vlt $commandEffective")

        val elapsed = measureTimeMillis {
            cli.exec(contentDirEffective, commandEffective)
        }

        return VaultSummary(commandEffective, contentDirEffective, elapsed)
    }
}
