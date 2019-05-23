package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.pkg.PackageDownloader
import com.cognifide.gradle.aem.tooling.clean.Cleaner
import com.cognifide.gradle.aem.tooling.vlt.VltRunner
import java.io.File
import java.util.regex.Pattern
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Sync : AemDefaultTask() {

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    @Internal
    var mode = Mode.of(aem.props.string("sync.mode") ?: Mode.COPY_AND_CLEAN.name)

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    var transfer = Transfer.of(aem.props.string("sync.transfer") ?: Transfer.PACKAGE_DOWNLOAD.name)

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
    var contentDir = aem.packageOptions.rootDir

    private val cleaner = Cleaner(project)

    private val downloader = PackageDownloader(aem)

    private val vlt = VltRunner(aem)

    private val filterRootFiles: List<File>
        get() {
            if (!contentDir.exists()) {
                logger.warn("JCR content directory does not exist: $contentDir")
                return listOf()
            }

            return filter.rootDirs(contentDir).map { normalizeRoot(it) }.distinct()
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

            if (!contentDir.exists()) {
                aem.notifier.notify("Cannot synchronize JCR content", "Directory does not exist: ${aem.packageOptions.jcrRootDir}")
                return
            }

            if (mode != Mode.CLEAN_ONLY) {
                when (transfer) {
                    Transfer.VLT_CHECKOUT -> transferUsingVltCheckout()
                    Transfer.PACKAGE_DOWNLOAD -> transferUsingPackageDownload()
                }
            }

            aem.notifier.notify("Synchronized JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(contentDir, project)}")
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
            cleaner.prepare(normalizeRoot(root))
        }
    }

    private fun transferUsingVltCheckout() {
        vlt.apply {
            contentDir = this@Sync.contentDir
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
        logger.info("Cleaning copied files using: $filter")

        filterRootFiles.forEach { root ->
            cleaner.beforeClean(root)
        }

        filterRootFiles.forEach { root ->
            cleaner.clean(root)
        }
    }

    private fun normalizeRoot(root: File): File {
        return File(manglePath(Patterns.normalizePath(root.path).substringBefore("/${Cleaner.JCR_CONTENT_NODE}")))
    }

    private fun manglePath(path: String): String {
        var mangledPath = path
        if (path.contains(":")) {
            val matcher = MANGLE_NAMESPACE_OUT_PATTERN.matcher(path)
            val buffer = StringBuffer()
            while (matcher.find()) {
                val namespace = matcher.group(1)
                matcher.appendReplacement(buffer, "/_${namespace}_")
            }
            matcher.appendTail(buffer)
            mangledPath = buffer.toString()
        }
        return mangledPath
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
        const val NAME = "sync"

        private val MANGLE_NAMESPACE_OUT_PATTERN: Pattern = Pattern.compile("/([^:/]+):")
    }
}