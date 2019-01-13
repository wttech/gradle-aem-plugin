package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.instance.LocalHandleOptions
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Create : Instance() {

    @Internal
    val options = LocalHandleOptions(aem, AemTask.temporaryDir(project, name))

    @Input
    var mode = aem.props.string("aem.create.mode") // TODO auto (most recent), from external jar, from recent internal jar

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

        val backupZip = findRecentBackup(options.zip)
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

    private fun findRecentBackup(zip: File?): File? {
        val external = if (zip == null) listOf() else listOf(zip)
        val internal = aem.tasks.named<Backup>(Backup.NAME).get().available

        return (external + internal).asSequence().sortedByDescending { it.name }.firstOrNull()
    }

    companion object {
        const val NAME = "aemCreate"
    }
}