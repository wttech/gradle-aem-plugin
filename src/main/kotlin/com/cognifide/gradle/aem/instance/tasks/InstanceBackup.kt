package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.ZipTask
import com.cognifide.gradle.aem.common.utils.Formats
import java.io.File
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.ZipEntryCompression

open class InstanceBackup : ZipTask() {

    /**
     * Determines what need to be done (backup zipped and uploaded or something else).
     */
    @Internal
    var mode: Mode = Mode.of(aem.props.string("instance.backup.mode")
            ?: Mode.ZIP_AND_UPLOAD.name)

    init {
        description = "Turns off local instance(s), archives to ZIP file, then turns on again."

        archiveBaseName.set(project.provider { "${project.rootProject.name}-${Formats.dateFileName()}" })
        archiveClassifier.set("backup")
        destinationDirectory.set(aem.temporaryDir("backup/local"))

        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED
    }

    @TaskAction
    override fun copy() {
        when (mode) {
            Mode.ZIP_ONLY -> zip()
            Mode.ZIP_AND_UPLOAD -> { zip(); upload(false) }
        }
    }

    private fun zip() {
        super.copy()
    }

    private fun upload(verbose: Boolean) {
        val dirUrl = aem.localInstanceManager.backup.uploadUrl
        if (dirUrl.isNullOrBlank()) {
            val message = "Cannot upload local instance backup as of URL is not defined."
            if (verbose) {
                throw InstanceException(message)
            } else {
                aem.logger.info(message)
                return
            }
        }

        val backupZip = archiveFile.get().asFile

        aem.logger.info("Uploading local instance(s) backup '$backupZip' to '$dirUrl'")
        aem.fileTransfer.uploadTo(dirUrl, backupZip)
    }

    @get:Internal
    val available: List<File>
        get() {
            return (destinationDirectory.asFile.get().listFiles { _, name ->
                name.endsWith("-${archiveClassifier.get()}.${archiveExtension.get()}")
            } ?: arrayOf()).toList()
        }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (!graph.hasTask(this)) {
            return
        }

        val uncreatedInstances = aem.localInstances.filter { !it.created }
        if (uncreatedInstances.isNotEmpty()) {
            throw InstanceException("Cannot create local instance backup, because there are instances not yet created: ${uncreatedInstances.names}")
        }
    }

    override fun projectEvaluated() {
        from(aem.localInstanceManager.rootDir)
    }

    enum class Mode {
        ZIP_ONLY,
        ZIP_AND_UPLOAD;

        companion object {
            fun of(name: String): Mode {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported instance backup mode: $name")
            }
        }
    }

    companion object {
        const val NAME = "instanceBackup"
    }
}
