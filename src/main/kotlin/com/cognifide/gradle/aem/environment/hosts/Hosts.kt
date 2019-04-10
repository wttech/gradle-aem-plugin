package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.lang3.SystemUtils

open class Hosts(
    private val list: List<Host>,
    private val filePath: String,
    private val permissionDeniedText: String,
    private val superUserRequestMessage: String
) {

    fun append() = try {
        FileOperations.amendFile(File(filePath)) { append(it) }
    } catch (e: FileNotFoundException) {
        if (e.message?.contains(permissionDeniedText) == true) {
            throw EnvironmentException("Failed to append hosts to $filePath file - permission denied." +
                    "\n$superUserRequestMessage", e)
        }
        throw EnvironmentException("Failed to append hosts to $filePath file.", e)
    }

    internal fun append(hostsFileContent: String): String {
        val lines = hostsFileContent.split(System.lineSeparator())
        val map = lines.map { it to null }.toList().toMap()
        val newHosts = list.filter { !map.containsKey(it.text) }.map { it.text }
        return assemble(lines + newHosts)
    }

    private fun assemble(lines: List<String>) =
            lines.joinToString(System.lineSeparator())

    companion object {
        fun create(list: List<Host>) = when {
            SystemUtils.IS_OS_WINDOWS -> HostsWindows(list)
            else -> HostsUnix(list)
        }
    }
}