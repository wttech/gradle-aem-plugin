package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.instance.tasks.Tail
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI
import org.apache.commons.io.FileUtils

class LogFiles(private val aem: AemExtension, private val taskName: String) {

    fun mainUri(instanceName: String) = uri(main(instanceName))

    fun clearMain(instanceName: String) = main(instanceName).bufferedWriter().use { it.write("") }

    fun clearSnapshots(instanceName: String) =
        FileUtils.deleteDirectory(AemTask.temporaryDir(aem.project, taskName, "$instanceName/snapshots"))

    fun writeToSnapshot(instanceName: String, writerBlock: (BufferedWriter) -> Unit): URI {
        val snapshotFile = snapshot(instanceName)
        snapshotFile.bufferedWriter().use(writerBlock)
        return uri(snapshotFile)
    }

    fun writeToMain(instanceName: String, writerBlock: (FileWriter) -> Unit) =
        FileWriter(main(instanceName).path, true).use(writerBlock)

    private fun main(instanceName: String) =
        AemTask.temporaryFile(aem.project, "$taskName/$instanceName", Tail.LOG_FILE)

    private fun snapshot(instanceName: String) =
        AemTask.temporaryFile(
            aem.project,
            "$taskName/$instanceName/snapshots",
            "${Formats.dateFileName()}-${Tail.LOG_FILE}"
        )

    private fun uri(file: File): URI = file.toURI()
}