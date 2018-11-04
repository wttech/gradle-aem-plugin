package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import java.io.File

class VltRunner(project: Project) {

    private val logger: Logger = project.logger

    private val aem = AemExtension.of(project)

    private val app = VltApp(project)

    @Input
    var command: String = project.findProperty("aem.vlt.command") as String? ?: ""

    @Input
    var commandProperties: Map<String, Any> = mapOf("config" to aem.config)

    @get:Internal
    val commandEffective: String
        get() = aem.props.expand(command, commandProperties)

    @InputDirectory
    var contentDir: File = project.file("src/main/content")

    @Input
    var contentRelativePath: String = project.findProperty("aem.vlt.path") as String? ?: ""

    @get:Internal
    val contentDirEffective: File
        get() {
            var workingPath = "$contentDir/${PackagePlugin.JCR_ROOT}"
            if (contentRelativePath.isNotBlank()) {
                workingPath = "$workingPath/$contentRelativePath"
            }

            return File(workingPath)
        }

    fun run() {
        if (commandEffective.isBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        logger.lifecycle("Working directory: $contentDirEffective")
        logger.lifecycle("Executing command: vlt $commandEffective")

        app.execute(commandEffective, contentDirEffective)
    }

}