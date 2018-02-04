package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class VltCommand(val project: Project) {

    val logger: Logger = project.logger

    val propertyParser = PropertyParser(project)

    val config = AemConfig.of(project)

    fun clean() {
        val config = AemConfig.of(project)
        val contentDir = File(config.contentPath)

        if (!contentDir.exists()) {
            logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
            return
        }

        val cleaner = VltCleaner(contentDir, logger)
        cleaner.removeVltFiles()
        cleaner.cleanupDotContent(config.vaultSkipProperties, config.vaultLineSeparatorString)
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

        val filter = determineFilter()
        val instance = determineInstance()

        val specificProps = mapOf(
                "instance" to instance,
                "filter" to filter.file.absolutePath
        )
        val fullCommand = PropertyParser(project).expand("${config.vaultGlobalOptions} $command".trim(), specificProps)

        app.execute(fullCommand)
        filter.clean()
    }

    fun determineFilter(): VltFilter {
        val config = AemConfig.of(project)
        val cmdFilterPath = propertyParser.string("aem.vlt.filter")
        val cmdFilterRoots = PropertyParser(project).list("aem.vlt.filterRoots")

        return if (!cmdFilterPath.isNullOrBlank()) {
            val cmdFilter = FileOperations.find(project, config.vaultPath, cmdFilterPath!!)
                    ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath")

            logger.debug("Using VLT filter specified as command line property")
            VltFilter(cmdFilter)
        } else if (cmdFilterRoots.isNotEmpty()) {
            logger.debug("Using VLT filter roots specified as command line property")
            VltFilter.temporary(project, cmdFilterRoots)
        } else {
            logger.debug("Using VLT filter specified in AEM build configuration")
            VltFilter(File(config.vaultFilterPath))
        }
    }

    fun determineInstance(): Instance {
        val cmdInstanceArg = propertyParser.string("aem.vlt.instance")
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = Instance.parse(cmdInstanceArg!!).first()
            cmdInstance.validate()

            logger.debug("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val authorInstance = Instance.filter(project, config.deployInstanceAuthorName).firstOrNull()
        if (authorInstance != null) {
            logger.debug("Using first instance matching filter '${config.deployInstanceAuthorName}': $authorInstance")
            return authorInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.debug("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor AEM config.")
    }

}