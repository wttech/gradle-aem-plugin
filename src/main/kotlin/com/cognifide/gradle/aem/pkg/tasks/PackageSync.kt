package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.common.pkg.vault.VaultClient
import com.cognifide.gradle.aem.pkg.tasks.sync.Cleaner
import com.cognifide.gradle.aem.pkg.tasks.sync.Downloader
import com.cognifide.gradle.common.utils.using
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageSync : AemDefaultTask() {

    /**
     * Determines what need to be done (content copied and clean or something else).
     */
    @Internal
    val mode = aem.obj.typed<Mode> {
        convention(Mode.COPY_AND_CLEAN)
        aem.prop.string("package.sync.mode")?.let { set(Mode.of(it)) }
    }

    fun mode(name: String) {
        mode.set(Mode.of(name))
    }

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    val transfer = aem.obj.typed<Transfer> {
        convention(Transfer.PACKAGE_DOWNLOAD)
        aem.prop.string("package.sync.transfer")?.let { set(Transfer.of(it)) }
    }

    fun transfer(name: String) {
        transfer.set(Transfer.of(name))
    }

    /**
     * Source instance from which JCR content will be copied.
     */
    @Internal
    val instance = aem.obj.typed<Instance> { convention(aem.obj.provider { aem.anyInstance }) }

    /**
     * Determines which content will be copied from source instance.
     */
    @Internal
    val filter = aem.obj.typed<FilterFile> { convention(aem.obj.provider { aem.filter }) }

    fun filter(path: String) {
        filter.set(aem.filter(path))
    }

    fun filter(file: File) {
        filter.set(aem.filter(file))
    }

    fun filter(file: Provider<File>) {
        filter.set(file.map { aem.filter(it) })
    }

    /**
     * Location of JCR content root to which content will be copied.
     */
    @Internal
    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    fun contentDir(rootPath: String) = contentDir(project.rootProject.layout.projectDirectory.dir(rootPath))

    fun contentDir(dir: File) {
        contentDir.set(dir)
        filter.set(project.provider {
            FilterFile.cmd(aem) ?: FilterFile(dir.resolve("${Package.VLT_PATH}/${FilterFile.BUILD_NAME}"))
        })
    }

    fun contentDir(dir: Directory) {
        contentDir.set(dir)
        filter.set(contentDir.map {
            aem.filter(it.file("${Package.VLT_PATH}/${FilterFile.BUILD_NAME}").asFile)
        })
    }

    private val filterRootFiles: List<File>
        get() = contentDir.get().asFile.run {
            if (!exists()) {
                logger.warn("JCR content directory does not exist: $this")
                listOf<File>()
            }

            filter.get().rootDirs(this)
        }

    fun cleaner(options: Cleaner.() -> Unit) = cleaner.using(options)

    @get:Internal
    val cleaner by lazy { Cleaner(aem) }

    fun vaultClient(options: VaultClient.() -> Unit) = vaultClient.using(options)

    @get:Internal
    val vaultClient by lazy {
        VaultClient(aem).apply {
            contentDir.convention(this@PackageSync.contentDir)
            command.convention(aem.obj.provider {
                "--credentials ${instance.get().credentialsString} checkout --force" +
                        " --filter ${filter.get().file} ${instance.get().httpUrl}/crx/server/crx.default"
            })
        }
    }

    fun downloader(options: Downloader.() -> Unit) = downloader.using(options)

    @get:Internal
    val downloader by lazy {
        Downloader(aem).apply {
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

        common.progress {
            step = "Initializing"

            try {
                contentDir.get().asFile.mkdirs()

                if (mode.get() != Mode.COPY_ONLY) {
                    step = "Preparing content"
                    prepareContent()
                }

                if (mode.get() != Mode.CLEAN_ONLY) {
                    when (transfer.get()) {
                        Transfer.VLT_CHECKOUT -> {
                            step = "Checking out content"
                            vaultClient.run()
                        }
                        Transfer.PACKAGE_DOWNLOAD -> {
                            step = "Downloading content"
                            downloader.download()
                        }
                        else -> {}
                    }
                }

                common.notifier.notify(
                    "Synchronized JCR content",
                    "Instance: ${instance.get().name}. Directory: ${Formats.rootProjectPath(contentDir.get().asFile, project)}"
                )
            } finally {
                step = "Cleaning content"
                if (mode.get() != Mode.COPY_ONLY) {
                    cleanContent()
                }
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
