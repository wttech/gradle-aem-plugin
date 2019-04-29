package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.FileNotFoundException
import org.gradle.internal.os.OperatingSystem

open class Hosts(
    private val defined: List<Host>,
    private val filePath: String,
    @JsonIgnore
    private val permissionDeniedText: String,
    @JsonIgnore
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
        val newHosts = defined.filter { !map.containsKey(it.text) }.map { it.text }
        return assemble(lines + newHosts)
    }

    private fun assemble(lines: List<String>) = lines.joinToString(System.lineSeparator())

    companion object {
        fun of(defined: List<Host>) = when {
            OperatingSystem.current().isWindows -> HostsWindows(defined)
            else -> HostsUnix(defined)
        }
    }
}