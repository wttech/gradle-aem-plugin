package com.cognifide.gradle.aem.tooling.tail.io

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.tooling.tail.TailException
import com.cognifide.gradle.aem.tooling.tasks.Tail
import java.io.*
import java.net.URI
import java.net.URL
import org.apache.commons.io.FileUtils
import org.gradle.util.GFileUtils

class LogFiles(private val aem: AemExtension) {

    fun mainUri(instanceName: String) = uri(main(instanceName))

    fun clearMain(instanceName: String) = main(instanceName).bufferedWriter().use { it.write("") }

    fun clearSnapshots(instanceName: String) =
            FileUtils.deleteDirectory(AemTask.temporaryDir(aem.project, Tail.NAME, "$instanceName/snapshots"))

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
            AemTask.temporaryFile(aem.project, "${Tail.NAME}/$instanceName/snapshots", "${Formats.dateFileName()}-error.log")

    private fun uri(file: File): URI = file.toURI()

    companion object {

        fun <T> readClasspathOrPath(resource: String, parser: (BufferedReader) -> T): T =
                BufferedReader(InputStreamReader(inputStream(resource))).use(parser)

        fun <T> optionalReadClasspathOrPath(resource: String, parser: (BufferedReader) -> T): T? =
                try {
                    readClasspathOrPath(resource, parser)
                } catch (e: TailException) {
                    null
                }

        private fun inputStream(resourcePath: String): InputStream {
            val resource: URL? = this::class.java.classLoader.getResource(resourcePath)
            if (resource != null) {
                return GFileUtils.openInputStream(File(resource.file))
            }
            val resourceStream: InputStream? = this::class.java.getResourceAsStream(resourcePath)
            if (resourceStream != null) {
                return resourceStream
            }
            val resourceFile = File(resourcePath)
            if (resourceFile.exists()) {
                return GFileUtils.openInputStream(resourceFile)
            }
            throw TailException("Cannot load blacklist from file: $resourcePath")
        }
    }
}