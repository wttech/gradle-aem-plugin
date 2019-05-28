package com.cognifide.gradle.aem.instance.tasks

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

    init {
        description = "Turns off local instance(s), archives to ZIP file."

        archiveBaseName.set(project.provider { "${project.rootProject.name}-${Formats.dateFileName()}" })
        archiveClassifier.set("backup")
        destinationDirectory.set(project.file("build/aem/backup/local"))

        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED
    }

    @Internal
    var uploadUrl = aem.props.string("backup.uploadUrl")

    @TaskAction
    override fun copy() {
        super.copy()
        upload()
        aem.tasks.named<InstanceUp>(InstanceUp.NAME).get().up()
    }

    private fun upload() {
        uploadUrl?.let { url ->
            val backupZip = archiveFile.get().asFile
            logger.info("Uploading backup: ${backupZip.path} to $url")
            aem.fileTransfer.upload(url, backupZip)
        }
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
            throw InstanceException("Cannot create backup of local instances, because there are instances not yet created: ${uncreatedInstances.names}")
        }
    }

    override fun projectEvaluated() {
        from(aem.localInstanceOptions.rootDir)
    }

    companion object {
        const val NAME = "instanceBackup"
    }
}
