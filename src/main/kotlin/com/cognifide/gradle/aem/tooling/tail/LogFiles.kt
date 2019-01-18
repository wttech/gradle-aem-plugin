package com.cognifide.gradle.aem.tooling.tail

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.tooling.tasks.Tail
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI

class LogFiles(private val aem: AemExtension) {

    fun mainUri(instanceName: String) = uri(main(instanceName))

    fun clearMain(instanceName: String) = main(instanceName).bufferedWriter().use { it.write("") }

    fun writeToSnapshot(instanceName: String, writerBlock: (BufferedWriter) -> Unit): URI {
        val snapshotFile = snapshot(instanceName)
        snapshotFile.bufferedWriter().use(writerBlock)
        return uri(snapshotFile)
    }

    fun writeToMain(instanceName: String, writerBlock: (FileWriter) -> Unit) =
            FileWriter(main(instanceName).path, true).use(writerBlock)

    private fun main(instanceName: String) =
            AemTask.temporaryFile(aem.project, "${Tail.NAME}/$instanceName", "error.log")

    private fun snapshot(instanceName: String) =
            AemTask .temporaryFile(aem.project, "${Tail.NAME}/$instanceName/snapshots", "${Formats.dateFileName()}-error.log")

    private fun uri(file: File): URI = file.toURI()
}