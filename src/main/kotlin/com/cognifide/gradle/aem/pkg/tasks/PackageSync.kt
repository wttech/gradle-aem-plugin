package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.common.pkg.vlt.VltClient
import com.cognifide.gradle.aem.pkg.tasks.sync.Cleaner
import com.cognifide.gradle.aem.pkg.tasks.sync.Downloader
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageSync : AemDefaultTask() {

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    @Internal
    var mode = Mode.of(aem.prop.string("package.sync.mode")
            ?: Mode.COPY_AND_CLEAN.name)

    fun mode(name: String) {
        mode = Mode.of(name)
    }

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    var transfer = Transfer.of(aem.prop.string("package.sync.transfer")
            ?: Transfer.PACKAGE_DOWNLOAD.name)

    fun transfer(name: String) {
        transfer = Transfer.of(name)
    }

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
    var contentDir = aem.packageOptions.contentDir

    private val filterRootFiles: List<File>
        get() {
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: $contentDir")
                return listOf()
            }

            return filter.rootDirs(contentDir)
        }

    private var vltOptions: VltClient.() -> Unit = {}

    fun vlt(options: VltClient.() -> Unit) {
        vltOptions = options
    }

    private val vlt by lazy { VltClient(aem).apply(vltOptions) }

    private var cleanerOptions: Cleaner.() -> Unit = {}

    fun cleaner(options: Cleaner.() -> Unit) {
        cleanerOptions = options
    }

    private val cleaner by lazy { Cleaner(aem).apply(cleanerOptions) }

    private var downloaderOptions: Downloader.() -> Unit = {}

    fun downloader(options: Downloader.() -> Unit) {
        downloaderOptions = options
    }

    private val downloader by lazy { Downloader(aem).apply(downloaderOptions) }

    init {
        description = "Check out then clean JCR content."
    }

    @TaskAction
    fun sync() {
        try {
            if (mode != Mode.COPY_ONLY) {
                prepareContent()
            }

            if (!contentDir.exists()) {
                common.notifier.notify("Cannot synchronize JCR content", "Directory does not exist: ${aem.packageOptions.jcrRootDir}")
                return
            }

            if (mode != Mode.CLEAN_ONLY) {
                when (transfer) {
                    Transfer.VLT_CHECKOUT -> transferUsingVltCheckout()
                    Transfer.PACKAGE_DOWNLOAD -> transferUsingPackageDownload()
                }
            }

            common.notifier.notify(
                    "Synchronized JCR content",
                    "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(contentDir, project)}"
            )
        } finally {
            if (mode != Mode.COPY_ONLY) {
                cleanContent()
            }
        }
    }

    private fun prepareContent() {
        logger.info("Preparing files to be cleaned up (before copying new ones) using: $filter")

        filterRootFiles.forEach { root ->
            logger.lifecycle("Preparing root: $root")
            cleaner.prepare(root)
        }
    }

    private fun transferUsingVltCheckout() {
        vlt.apply {
            contentDir = this@PackageSync.contentDir
            command = "--credentials ${instance.credentialsString} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
    }

    private fun transferUsingPackageDownload() {
        downloader.apply {
            instance = this@PackageSync.instance
            filter = this@PackageSync.filter
            download()
        }
    }

    private fun cleanContent() {
        logger.info("Cleaning copied files using: $filter")

        filterRootFiles.forEach { root ->
            cleaner.beforeClean(root)
        }

        filterRootFiles.forEach { root ->
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
        const val NAME = "packageSync"
    }
}
