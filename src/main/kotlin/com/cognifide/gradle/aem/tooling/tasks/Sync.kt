package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.pkg.PackageDownloader
import com.cognifide.gradle.aem.tooling.clean.Cleaner
import com.cognifide.gradle.aem.tooling.vlt.VltRunner
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Sync : AemDefaultTask() {

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    @Internal
    var mode = Mode.of(aem.props.string("aem.sync.mode") ?: Mode.COPY_AND_CLEAN.name)

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    var transfer = Transfer.of(aem.props.string("aem.sync.transfer") ?: Transfer.PACKAGE_DOWNLOAD.name)

    /**
     * Source instance from which JCR content will be copied.
     */
    @Internal
    var instance = aem.anyInstance

    /**
     * Determines which content will be copied from source instance.
     */
    @Internal
    var filter = aem.filter

    /**
     * Location of JCR content root to which content will be copied.
     */
    @Internal
    var contentPath = aem.config.packageRoot

    private val cleaner = Cleaner(project)

    private val downloader = PackageDownloader(aem, AemTask.temporaryDir(project, name))

    private val vlt = VltRunner(aem)

    private val filterRootDirs: List<File>
        get() {
            val contentDir = project.file(contentPath)
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: $contentPath")
                return listOf()
            }

            return filter.rootDirs(contentDir)
        }

    fun cleaner(options: Cleaner.() -> Unit) {
        cleaner.apply(options)
    }

    fun download(options: PackageDownloader.() -> Unit) {
        downloader.apply(options)
    }

    fun vlt(options: VltRunner.() -> Unit) {
        vlt.apply(options)
    }

    init {
        description = "Check out then clean JCR content."
    }

    @TaskAction
    fun sync() {
        try {
            if (mode != Mode.COPY_ONLY) {
                prepareContent()
            }

            if (!File(contentPath).exists()) {
                aem.notifier.notify("Cannot synchronize JCR content", "Directory does not exist: ${aem.config.packageJcrRoot}")
                return
            }

            if (mode != Mode.CLEAN_ONLY) {
                when (transfer) {
                    Transfer.VLT_CHECKOUT -> transferUsingVltCheckout()
                    Transfer.PACKAGE_DOWNLOAD -> transferUsingPackageDownload()
                }
            }

            aem.notifier.notify("Synchronized JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(contentPath, project)}")
        } finally {
            if (mode != Mode.COPY_ONLY) {
                cleanContent()
            }
        }
    }
    private fun prepareContent() {
        logger.info("Preparing files to be cleaned up (before copying new ones) using filter: $filter")

        filterRootDirs.forEach { root ->
            logger.lifecycle("Preparing root: $root")
            cleaner.prepare(root)
        }
    }

    private fun transferUsingVltCheckout() {
        vlt.apply {
            contentPath = this@Sync.contentPath
            command = "--credentials ${instance.credentials} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
    }

    private fun transferUsingPackageDownload() {
        downloader.apply {
            instance = this@Sync.instance
            filter = this@Sync.filter

            download()
        }
    }

    private fun cleanContent() {
        logger.info("Cleaning using $filter")

        filterRootDirs.forEach { root ->
            logger.lifecycle("Cleaning root: $root")
            cleaner.clean(root)
        }
    }

    enum class Transfer {
        VLT_CHECKOUT,
        PACKAGE_DOWNLOAD;

        companion object {
            fun of(name: String): Transfer {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported sync transport: $name")
            }
        }
    }

    enum class Mode {
        COPY_AND_CLEAN,
        CLEAN_ONLY,
        COPY_ONLY;

        companion object {
            fun of(name: String): Mode {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported sync mode: $name")
            }
        }
    }

    companion object {
        val NAME = "aemSync"
    }
}