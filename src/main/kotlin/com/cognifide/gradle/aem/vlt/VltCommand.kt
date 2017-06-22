package com.cognifide.gradle.aem.vlt

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import java.io.File

object VltCommand {

    fun clean(project: Project) {
        val config = AemConfig.of(project)
        val contentDir = File(config.contentPath)

        if (!contentDir.exists()) {
            project.logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
            return
        }

        val cleaner = VltCleaner(contentDir, project.logger)
        cleaner.removeVltFiles()
        cleaner.cleanupDotContent(config.vaultSkipProperties, config.vaultLineSeparator)
    }

    fun checkout(project: Project) {
        val config = AemConfig.of(project)
        val contentDir = File(config.contentPath)

        if (!contentDir.exists()) {
            project.logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        raw(project, "checkout --force --filter \${filter} \${instance.url}")
    }

    fun raw(project: Project, command: String) {
        val app = VltApp(project)
        val config = AemConfig.of(project)
        val specificProps = mapOf(
                "instance" to determineInstance(project),
                "filter" to determineFilter(project).absolutePath
        )
        val fullCommand = PropertyParser(project).expand("${config.vaultGlobalOptions} $command".trim(), specificProps)

        app.execute(fullCommand)
    }

    fun determineFilter(project: Project): File {
        val config = AemConfig.of(project)
        var filter = File(config.vaultFilterPath)
        val cmdFilterPath = project.properties["aem.vlt.filter"] as String?

        if (!cmdFilterPath.isNullOrBlank()) {
            val cmdFilter = project.file(cmdFilterPath)
            if (!cmdFilter.exists()) {
                throw VltException("Vault check out filter file does not exist at path: ${cmdFilter.absolutePath}")
            }

            filter = cmdFilter
        }

        return filter
    }

    fun determineInstance(project: Project): AemInstance {
        val cmdInstanceArg = project.properties["aem.vlt.instance"] as String?
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = AemInstance.parse(cmdInstanceArg!!).first()
            cmdInstance.validate()

            return cmdInstance
        }

        return AemInstance.filter(project, AemInstance.FILTER_AUTHOR).first()
    }

}