package com.cognifide.gradle.aem.base.tasks

import com.cognifide.gradle.aem.base.clean.Cleaner
import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.Formats
import java.io.File
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Clean : AemDefaultTask() {

    init {
        description = "Clean checked out JCR content."
    }

    @Input
    var contentPath = aem.config.packageRoot

    @Nested
    val filter = aem.filter

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
    val cleaner = Cleaner(project)

    @TaskAction
    fun perform() {
        afterCheckout()
        aem.notifier.notify("Cleaned JCR content", "Directory: ${Formats.rootProjectPath(contentPath, project)}")
    }

    fun options(configurer: Cleaner.() -> Unit) {
        cleaner.apply(configurer)
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this@Clean)) {
            project.tasks.named(Checkout.NAME).configure {
                doFirst { beforeCheckout() }
            }
        }
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