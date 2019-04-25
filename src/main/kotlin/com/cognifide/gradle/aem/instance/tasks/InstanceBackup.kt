package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.file.downloader.FileTransfer
import com.cognifide.gradle.aem.common.file.downloader.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.downloader.SmbFileTransfer
import com.cognifide.gradle.aem.common.tasks.ZipTask
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File

open class InstanceBackup : ZipTask() {

    init {
        description = "Turns off local instance(s), archives to ZIP file, then turns on again."

        archiveBaseName.set(project.provider { "${project.rootProject.name}-${Formats.dateFileName()}" })
        archiveClassifier.set("backup")

        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED
    }

    @Input
    var uploadUrl = aem.props.string("aem.backup.uploadUrl")

    @TaskAction
    override fun copy() {
        super.copy()
        upload()
    }

    private fun upload() {
        uploadUrl?.let { url ->
            val targetUrl = "${url.trimEnd('/')}/$archiveFileName"
            val backupZip = archiveFile.get().asFile
            logger.lifecycle("Uploading backup '${backupZip.path}' to '$targetUrl'")
            fileTransfer(url).upload(backupZip, targetUrl)
        }
    }

    @get:Internal
    val available: List<File>
        get() {
            return (destinationDirectory.asFile.get().listFiles { _, name ->
                name.endsWith("-$archiveClassifier.$archiveExtension")
            } ?: arrayOf()).ifEmpty { arrayOf() }.toList()
        }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (!graph.hasTask(this)) {
            return
        }

        val uncreatedInstances = aem.localInstances.filter { !it.created }
        if (uncreatedInstances.isNotEmpty()) {
            throw InstanceException("Cannot create backup of local instances, because there are instances not yet created: ${uncreatedInstances.names}")
        }
    }

    override fun projectEvaluated() {
        from(aem.config.localInstanceOptions.rootDir)
    }

    private fun fileTransfer(url: String): FileTransfer {
        return when {
            SftpFileTransfer.handles(url) -> SftpFileTransfer(aem.project)
            SmbFileTransfer.handles(url) -> SmbFileTransfer(aem.project)
            else -> throw AemException("Cannot upload backup to URL: '$url'. Only SMB and SFTP URLs are supported.")
        }
    }

    companion object {
        const val NAME = "instanceBackup"
    }
}
