package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.instance.tail.TailOptions
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI
import org.apache.commons.io.FileUtils

class LogFiles(private val options: TailOptions, private val taskName: String) {

    private val aem = options.aem

    fun main(instanceName: String): File {
        return AemTask.temporaryFile(aem.project, "$taskName/$instanceName", options.logFile())
    }

    fun incident(instanceName: String): File {
        return AemTask.temporaryFile(
                aem.project,
                "$taskName/$instanceName/$INCIDENT_DIR",
                "${Formats.dateFileName()}-${options.logFile()}"
        )
    }

    fun clearMain(instanceName: String) = main(instanceName).bufferedWriter().use { it.write("") }

    fun clearIncidents(instanceName: String) {
        FileUtils.deleteDirectory(AemTask.temporaryDir(aem.project, taskName, "$instanceName/$INCIDENT_DIR"))
    }

    fun writeToIncident(instanceName: String, writerBlock: (BufferedWriter) -> Unit): URI {
        return incident(instanceName).apply { bufferedWriter().use(writerBlock) }.toURI()
    }

    fun writeToMain(instanceName: String, writerBlock: (FileWriter) -> Unit) {
        FileWriter(main(instanceName).path, true).use(writerBlock)
    }

    fun lock() {
        lock(lockFile)
    }

    fun isLocked(): Boolean {
        return lockFile.exists() && lockFile.lastModified() + options.lockInterval > System.currentTimeMillis()
    }

    fun isNotifiable(): Boolean {
        if (isLocked()) {
            return false
        }

        if (notifyFile.exists() && notifyFile.lastModified() + options.usageInterval > System.currentTimeMillis()) {
            return false
        }

        lock(notifyFile)

        return true
    }

    private fun lock(file: File) {
        if (!file.exists()) {
            file.bufferedWriter().use { it.write("") }
        } else {
            file.setLastModified(System.currentTimeMillis())
        }
    }

    private val lockFile: File
        get() = AemTask.temporaryFile(aem.project, taskName, LOCK_FILE)

    private val notifyFile: File
        get() = AemTask.temporaryFile(aem.project, taskName, NOTIFY_FILE)

    companion object {

        const val LOCK_FILE = "tailer.lock"

        const val NOTIFY_FILE = "tailer.notify"

        const val INCIDENT_DIR = "incidents"
    }
}