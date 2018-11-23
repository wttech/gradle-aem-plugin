package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.pkg.PackagePlugin
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class VltRunner(project: Project) {

    private val aem = AemExtension.of(project)

    private val app = VltApp(project)

    @Input
    var command: String = aem.props.string("aem.vlt.command", "")

    @Input
    var commandProperties: Map<String, Any> = mapOf("config" to aem.config)

    @get:Internal
    val commandEffective: String
        get() = aem.props.expand(command, commandProperties)

    @Input
    var contentPath: String = aem.config.packageRoot

    @Input
    var contentRelativePath: String = aem.props.string("aem.vlt.path", "")

    @get:Internal
    val contentDirEffective: File
        get() {
            var workingPath = "$contentPath/${PackagePlugin.JCR_ROOT}"
            if (contentRelativePath.isNotBlank()) {
                workingPath = "$workingPath/$contentRelativePath"
            }

            return File(workingPath)
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