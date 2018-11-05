package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.internal.Formats
import org.gradle.api.tasks.*
import java.io.File

open class CleanTask : AemDefaultTask() {

    init {
        description = "Clean checked out JCR content."

        project.run {
            gradle.taskGraph.whenReady {
                if (it.hasTask(tasks.getByName(CheckoutTask.NAME))) {
                    beforeCheckout()
                }
            }
        }
    }

    @Input
    var contentPath = aem.config.packageRoot

    @Nested
    val filter = aem.filter()

    @get:Internal
    val filterRootDirs: List<File>
        get() {
            val contentDir = project.file(contentPath)
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: $contentPath")
                return listOf()
            }

            return filter.rootDirs(contentDir)
        }

    @Internal
    var filterRootPrepare: (File) -> Unit = { cleaner.prepare(it) }

    @Internal
    var filterRootClean: (File) -> Unit = { cleaner.clean(it) }

    @Nested
    val cleaner = VltCleaner(project)

    @TaskAction
    fun perform() {
        afterCheckout()
        aem.notifier.notify("Cleaned JCR content", "Directory: ${Formats.rootProjectPath(contentPath, project)}")
    }

    fun cleaner(configurer: VltCleaner.() -> Unit) {
        cleaner.apply(configurer)
    }

    private fun beforeCheckout() {
        logger.info("Preparing files to be cleaned up (before checking out new ones) using filter: $filter")

        filterRootDirs.forEach { root ->
            logger.lifecycle("Preparing root: $root")
            filterRootPrepare(root)
        }
    }

    private fun afterCheckout() {
        logger.info("Cleaning using $filter")

        filterRootDirs.forEach { root ->
            logger.lifecycle("Cleaning root: $root")
            filterRootClean(root)
        }
    }

    companion object {
        const val NAME = "aemClean"
    }

}