package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class VltCommand(val project: Project) {

    val logger: Logger = project.logger

    fun clean() {
        val config = AemConfig.of(project)
        val contentDir = File(config.contentPath)

        if (!contentDir.exists()) {
            logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
            return
        }

        val cleaner = VltCleaner(contentDir, logger)
        cleaner.removeVltFiles()
        cleaner.cleanupDotContent(config.vaultSkipProperties, config.vaultLineSeparator)
    }

    fun checkout() {
        val config = AemConfig.of(project)
        val contentDir = File(config.contentPath)

        if (!contentDir.exists()) {
            logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        raw("checkout --force --filter {{filter}} {{instance.httpUrl}}/crx/server/crx.default")
    }

    fun raw(command: String) {
        val app = VltApp(project)
        val config = AemConfig.of(project)
        val specificProps = mapOf(
                "instance" to determineInstance(),
                "filter" to determineFilter().absolutePath
        )
        val fullCommand = PropertyParser(project).expand("${config.vaultGlobalOptions} $command".trim(), specificProps)

        app.execute(fullCommand)
    }

    fun determineFilter(): File {
        val config = AemConfig.of(project)
        var filter = File(config.vaultFilterPath)
        val cmdFilterPath = project.properties["aem.vlt.filter"] as String?

        if (!cmdFilterPath.isNullOrBlank()) {
            val cmdFilter = FileOperations.find(project, config.vaultPath, cmdFilterPath!!)
                    ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath")

            filter = cmdFilter
        }

        return filter
    }

    fun determineInstance(): Instance {
        val cmdInstanceArg = project.properties["aem.vlt.instance"] as String?
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = Instance.parse(cmdInstanceArg!!).first()
            cmdInstance.validate()

            logger.info("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val authorInstance = Instance.filter(project, Instance.FILTER_AUTHOR).firstOrNull()
        if (authorInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_AUTHOR}': $authorInstance")
            return authorInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor AEM config.")
    }

}