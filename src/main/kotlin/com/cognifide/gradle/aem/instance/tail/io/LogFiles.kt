package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.instance.tail.InstanceTailer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI
import org.apache.commons.io.FileUtils

class LogFiles(private val tailer: InstanceTailer) {

    fun main(instanceName: String) = tailer.logStorageDir.get().asFile
            .resolve("$instanceName/${tailer.logFile}")
            .apply { parentFile.mkdirs() }

    fun incidentDir(instanceName: String) = tailer.logStorageDir.get().asFile
            .resolve("$instanceName/$INCIDENT_DIR")

    fun incidentFile(instanceName: String) = incidentDir(instanceName).resolve("${Formats.dateFileName()}-${tailer.logFile}")
            .apply { parentFile.mkdirs() }

    fun clearMain(instanceName: String) = main(instanceName).bufferedWriter().use { it.write("") }

    fun clearIncidents(instanceName: String) {
        FileUtils.deleteDirectory(incidentDir(instanceName))
    }

    fun writeToIncident(instanceName: String, writerBlock: (BufferedWriter) -> Unit): URI {
        return incidentFile(instanceName).apply { bufferedWriter().use(writerBlock) }.toURI()
    }

    fun writeToMain(instanceName: String, writerBlock: (FileWriter) -> Unit) {
        FileWriter(main(instanceName).path, true).use(writerBlock)
    }

    fun lock() {
        lock(lockFile)
    }

    fun isLocked(): Boolean {
        return lockFile.exists() && lockFile.lastModified() + tailer.lockInterval > System.currentTimeMillis()
    }

    private fun lock(file: File) {
        if (!file.exists()) {
            file.bufferedWriter().use { it.write("") }
        } else {
            file.setLastModified(System.currentTimeMillis())
        }
    }

    private val lockFile: File get() = tailer.logStorageDir.get().asFile.resolve(LOCK_FILE).apply { parentFile.mkdirs() }

    companion object {

        const val LOCK_FILE = "tailer.lock"

        const val INCIDENT_DIR = "incidents"
    }
}
