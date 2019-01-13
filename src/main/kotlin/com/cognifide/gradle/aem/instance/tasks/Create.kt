package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.instance.LocalHandleOptions
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Create : Instance() {

    @Internal
    val options = LocalHandleOptions(aem, AemTask.temporaryDir(project, name))

    init {
        description = "Creates local AEM instance(s)."
    }

    fun options(configurer: LocalHandleOptions.() -> Unit) {
        options.apply(configurer)
    }

    @TaskAction
    fun create() {
        val handles = localHandles.filter { !it.created }
        if (handles.isEmpty()) {
            logger.info("No instance(s) to create")
            return
        }

        logger.info("Creating instances: ${handles.names}")

        val backupZip = options.zip
        if (backupZip != null) {
            val instanceRoot = File(aem.config.instanceRoot)
            aem.logger.info("Extracting files from backup ZIP '$backupZip' to directory '$instanceRoot'")
            aem.progress(FileOperations.zipCount(backupZip)) {
                FileOperations.zipUnpack(backupZip, instanceRoot) { increment("Extracting file '$it'") }
            }
        } else {
            aem.progress(handles.size) {
                handles.onEachApply {
                    increment("Instance '${instance.name}'") {
                        create(options)
                    }
                }
            }
        }

        aem.notifier.notify("Instance(s) created", "Which: ${handles.names}")
    }

    private fun findBackup(instanceFiles: List<File>): File? {
        return instanceFiles.find { it.extension == "zip" }
    }

    companion object {
        const val NAME = "aemCreate"

        const val DOWNLOAD_DIR = "download"
    }
}