package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.SlingException
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.pkg.vault.FilterFile
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.sling.common.pkg.vault.VaultClient
import com.cognifide.gradle.sling.pkg.tasks.sync.Cleaner
import com.cognifide.gradle.sling.pkg.tasks.sync.Downloader
import com.cognifide.gradle.common.utils.using
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageSync : SlingDefaultTask() {

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    @Internal
    val mode = sling.obj.typed<Mode> {
        convention(Mode.COPY_AND_CLEAN)
        sling.prop.string("package.sync.mode")?.let { set(Mode.of(it)) }
    }

    fun mode(name: String) {
        mode.set(Mode.of(name))
    }

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    val transfer = sling.obj.typed<Transfer> {
        convention(Transfer.PACKAGE_DOWNLOAD)
        sling.prop.string("package.sync.transfer")?.let { set(Transfer.of(it)) }
    }

    fun transfer(name: String) {
        transfer.set(Transfer.of(name))
    }

    /**
     * Source instance from which JCR content will be copied.
     */
    @Internal
    val instance = sling.obj.typed<Instance> { convention(sling.obj.provider { sling.anyInstance }) }

    /**
     * Determines which content will be copied from source instance.
     */
    @Internal
    val filter = sling.obj.typed<FilterFile> { convention(sling.obj.provider { sling.filter }) }

    /**
     * Location of JCR content root to which content will be copied.
     */
    @Internal
    val contentDir = sling.obj.dir { convention(sling.packageOptions.contentDir) }

    private val filterRootFiles: List<File>
        get() = contentDir.get().asFile.run {
            if (!exists()) {
                logger.warn("JCR content directory does not exist: $this")
                listOf<File>()
            }

            filter.get().rootDirs(this)
        }

    fun cleaner(options: Cleaner.() -> Unit) = cleaner.using(options)

    private val cleaner = Cleaner(sling)

    fun vaultClient(options: VaultClient.() -> Unit) = vaultClient.using(options)

    private val vaultClient by lazy {
        VaultClient(sling).apply {
            contentDir.convention(this@PackageSync.contentDir)
            command.convention(sling.obj.provider {
                "--credentials ${instance.get().credentialsString} checkout --force" +
                        " --filter ${filter.get().file} ${instance.get().httpUrl}/crx/server/crx.default"
            })
        }
    }

    fun downloader(options: Downloader.() -> Unit) = downloader.using(options)

    private val downloader by lazy {
        Downloader(sling).apply {
            instance.convention(this@PackageSync.instance)
            filter.convention(this@PackageSync.filter)
            extractDir.convention(contentDir.dir(Package.JCR_ROOT))

            definition {
                destinationDirectory.convention(project.layout.buildDirectory.dir(this@PackageSync.name))
                archiveFileName.convention("downloader.zip")
            }
        }
    }

    @TaskAction
    fun sync() {
        if (mode.get() != Mode.CLEAN_ONLY) {
            instance.get().examine()
        }

        try {
            contentDir.get().asFile.mkdirs()

            if (mode.get() != Mode.COPY_ONLY) {
                prepareContent()
            }

            if (mode.get() != Mode.CLEAN_ONLY) {
                when (transfer.get()) {
                    Transfer.VLT_CHECKOUT -> vaultClient.run()
                    Transfer.PACKAGE_DOWNLOAD -> downloader.download()
                    else -> {}
                }
            }

            common.notifier.notify(
                    "Synchronized JCR content",
                    "Instance: ${instance.get().name}. Directory: ${Formats.rootProjectPath(contentDir.get().asFile, project)}"
            )
        } finally {
            if (mode.get() != Mode.COPY_ONLY) {
                cleanContent()
            }
        }
    }

    private fun prepareContent() {
        logger.info("Preparing files to be cleaned up (before copying new ones) using: ${filter.get()}")

        filterRootFiles.forEach { root ->
            cleaner.prepare(root)
        }
    }

    private fun cleanContent() {
        logger.info("Cleaning copied files using: ${filter.get()}")

        filterRootFiles.forEach { root ->
            cleaner.beforeClean(root)
        }

        filterRootFiles.forEach { root ->
            cleaner.clean(root)
        }
    }

    init {
        description = "Check out then clean JCR content."
    }

    enum class Transfer {
        VLT_CHECKOUT,
        PACKAGE_DOWNLOAD;

        companion object {
            fun of(name: String): Transfer {
                return values().find { it.name.equals(name, true) }
                        ?: throw SlingException("Unsupported sync transport: $name")
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
                        ?: throw SlingException("Unsupported sync mode: $name")
            }
        }
    }

    companion object {
        const val NAME = "packageSync"
    }
}
