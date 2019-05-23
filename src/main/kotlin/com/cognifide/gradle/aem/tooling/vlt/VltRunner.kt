package com.cognifide.gradle.aem.tooling.vlt

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.pkg.Package
import java.io.File

class VltRunner(val aem: AemExtension) {

    private val app = VltApp(aem.project)

    var command: String = aem.props.string("vlt.command") ?: ""

    var commandProperties: Map<String, Any> = mapOf("aem" to aem)

    val commandEffective: String
        get() = aem.props.expand(command, commandProperties)

    var contentDir: File = aem.packageOptions.rootDir

    var contentRelativePath: String = aem.props.string("vlt.path") ?: ""

    val contentDirEffective: File
        get() {
            var workingDir = File(contentDir, Package.JCR_ROOT)
            if (contentRelativePath.isNotBlank()) {
                workingDir = File(workingDir, contentRelativePath)
            }

            return workingDir
        }

    fun run() {
        if (commandEffective.isBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        aem.logger.lifecycle("Working directory: $contentDirEffective")
        aem.logger.lifecycle("Executing command: vlt $commandEffective")

        app.execute(commandEffective, contentDirEffective)
    }
}