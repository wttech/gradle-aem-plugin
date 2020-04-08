package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.cli.CliApp
import org.apache.commons.lang3.time.StopWatch
import java.io.File

class VaultClient(val aem: AemExtension) {

    private val app = CliApp(aem).apply {
        dependencyNotation.apply {
            convention(aem.commonOptions.archiveExtension.map { "org.apache.jackrabbit.vault:vault-cli:3.4.0:bin@$it" })
            aem.prop.string(("vault.cli"))?.let { set(it) }
        }
        executable.set(aem.commonOptions.scriptExtension.map { "vault-cli-3.4.0/bin/vlt$it" })
    }

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

        val stopWatch = StopWatch().apply { start() }
        app.exec(contentDirEffective, commandEffective)
        stopWatch.stop()

        return VaultSummary(commandEffective, contentDirEffective, stopWatch.time)
    }
}
