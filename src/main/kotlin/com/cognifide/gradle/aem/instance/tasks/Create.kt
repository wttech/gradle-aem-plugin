package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.LocalHandleOptions
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Create : InstanceTask() {

    @Nested
    val options = LocalHandleOptions(project)

    @Internal
    val instanceFileResolver = FileResolver(aem, AemTask.temporaryDir(project, name, DOWNLOAD_DIR))

    @get:Internal
    val instanceFiles: List<File>
        get() = instanceFileResolver.allFiles()

    init {
        description = "Creates local AEM instance(s)."

        instanceFilesByProperties()
    }

    private fun instanceFilesByProperties() {
        val jarUrl = aem.props.string("aem.create.jarUrl")
        if (!jarUrl.isNullOrBlank()) {
            instanceFileResolver.url(jarUrl)
        }

        val licenseUrl = aem.props.string("aem.create.licenseUrl")
        if (!licenseUrl.isNullOrBlank()) {
            instanceFileResolver.url(licenseUrl)
        }
    }

    fun instanceFiles(configurer: FileResolver.() -> Unit) {
        instanceFileResolver.apply(configurer)
    }

    @TaskAction
    fun create() {
        if (localHandles.isEmpty() || localHandles.all { it.created }) {
            logger.info("No instances to create")
            return
        }

        logger.info("Creating instances")
        aem.parallelWith(localHandles) { create(options, instanceFiles) }

        aem.notifier.notify("Instance(s) created", "Which: ${localHandles.names}")
    }

    companion object {
        const val NAME = "aemCreate"

        const val DOWNLOAD_DIR = "download"
    }
}